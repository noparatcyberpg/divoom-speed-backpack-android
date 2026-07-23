package com.divoomspeed.backpack.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.divoomspeed.backpack.MainActivity
import com.divoomspeed.backpack.bluetooth.AutoDetectDivoomTransport
import com.divoomspeed.backpack.bluetooth.BluetoothConnectionState
import com.divoomspeed.backpack.bluetooth.DivoomTransport
import com.divoomspeed.backpack.bluetooth.FakeDivoomTransport
import com.divoomspeed.backpack.data.AppSettings
import com.divoomspeed.backpack.data.SettingsRepository
import com.divoomspeed.backpack.location.FusedSpeedTracker
import com.divoomspeed.backpack.location.SpeedReading
import com.divoomspeed.backpack.location.SpeedTracker
import com.divoomspeed.backpack.logging.DebugLogger
import com.divoomspeed.backpack.protocol.DivoomProtocolEncoder
import com.divoomspeed.backpack.protocol.LegacyDivoomProtocolEncoder
import com.divoomspeed.backpack.renderer.DefaultSpeedImageRenderer
import com.divoomspeed.backpack.renderer.DisplayConfig
import com.divoomspeed.backpack.renderer.DisplayOrientation
import com.divoomspeed.backpack.renderer.PixelFrame
import com.divoomspeed.backpack.renderer.SpeedColorMode
import com.divoomspeed.backpack.renderer.SpeedImageRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SpeedDisplayService : Service() {

    inner class SpeedBinder : Binder() {
        fun getService(): SpeedDisplayService = this@SpeedDisplayService
    }

    private val binder = SpeedBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var speedTracker: SpeedTracker
    private lateinit var transport: DivoomTransport
    private val renderer: SpeedImageRenderer = DefaultSpeedImageRenderer()
    private var encoder: DivoomProtocolEncoder = LegacyDivoomProtocolEncoder()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isSendPaused = MutableStateFlow(false)
    val isSendPaused: StateFlow<Boolean> = _isSendPaused.asStateFlow()

    private val _currentSpeed = MutableStateFlow(SpeedReading())
    val currentSpeed: StateFlow<SpeedReading> = _currentSpeed.asStateFlow()

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private var currentSettings = AppSettings()
    private var renderLoopJob: Job? = null
    private var autoReconnectJob: Job? = null
    private var lastSentSpeed: Int? = null

    companion object {
        const val CHANNEL_ID = "speed_display_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "action.START"
        const val ACTION_STOP = "action.STOP"
        const val ACTION_CONNECT = "action.CONNECT"
        const val ACTION_DISCONNECT = "action.DISCONNECT"
        const val ACTION_PAUSE_SEND = "action.PAUSE_SEND"
        const val ACTION_RESUME_SEND = "action.RESUME_SEND"
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        speedTracker = FusedSpeedTracker(this)
        transport = AutoDetectDivoomTransport(this)

        createNotificationChannel()

        // Observe settings changes
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                speedTracker.setSmoothingEnabled(settings.speedSmoothing)
                if (settings.demoMode && transport !is FakeDivoomTransport) {
                    transport = FakeDivoomTransport()
                }
            }
        }

        // Observe speed updates
        serviceScope.launch {
            speedTracker.speedState.collect { reading ->
                _currentSpeed.value = reading
                updateNotification()
            }
        }

        // Observe transport connection state
        serviceScope.launch {
            transport.connectionState.collect { state ->
                _connectionState.value = state
                updateNotification()
                if (state is BluetoothConnectionState.Connected) {
                    startRenderLoop()
                } else if (state is BluetoothConnectionState.Idle || state is BluetoothConnectionState.Error) {
                    stopRenderLoop()
                    if (currentSettings.autoReconnect && _isServiceRunning.value) {
                        scheduleAutoReconnect()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        DebugLogger.i("Service", "SpeedDisplayService received action: $action")

        when (action) {
            ACTION_START -> startServiceInternal()
            ACTION_STOP -> stopServiceInternal()
            ACTION_CONNECT -> connectDevice()
            ACTION_DISCONNECT -> disconnectDevice()
            ACTION_PAUSE_SEND -> _isSendPaused.value = true
            ACTION_RESUME_SEND -> _isSendPaused.value = false
        }

        return START_STICKY
    }

    private fun startServiceInternal() {
        if (_isServiceRunning.value) return
        _isServiceRunning.value = true

        val notification = buildNotification("Service started", "Initializing GPS & Bluetooth...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            speedTracker.start()
            if (currentSettings.deviceAddress.isNotBlank()) {
                connectDevice()
            }
        }
    }

    private fun stopServiceInternal() {
        _isServiceRunning.value = false
        stopRenderLoop()
        autoReconnectJob?.cancel()

        serviceScope.launch {
            speedTracker.stop()
            transport.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        DebugLogger.i("Service", "SpeedDisplayService stopped")
    }

    private fun connectDevice() {
        val address = currentSettings.deviceAddress
        if (address.isBlank()) {
            DebugLogger.w("Service", "No MAC address configured in settings")
            return
        }
        autoReconnectJob?.cancel()
        serviceScope.launch {
            transport.connect(address)
        }
    }

    private fun disconnectDevice() {
        autoReconnectJob?.cancel()
        serviceScope.launch {
            transport.disconnect()
        }
    }

    private fun startRenderLoop() {
        stopRenderLoop()
        renderLoopJob = serviceScope.launch {
            DebugLogger.i("Service", "Starting Realtime Speed Display Loop (Interval: ${currentSettings.updateIntervalMs} ms)")
            while (_isServiceRunning.value && connectionState.value is BluetoothConnectionState.Connected) {
                val reading = _currentSpeed.value
                val displaySpeed = reading.displaySpeedKmh

                if (!_isSendPaused.value && displaySpeed != lastSentSpeed) {
                    val colorMode = try {
                        SpeedColorMode.valueOf(currentSettings.colorMode)
                    } catch (e: Exception) {
                        SpeedColorMode.SPEED_BASED
                    }
                    val orientation = when (currentSettings.rotationDegrees) {
                        90 -> DisplayOrientation.ROTATION_90
                        180 -> DisplayOrientation.ROTATION_180
                        270 -> DisplayOrientation.ROTATION_270
                        else -> DisplayOrientation.ROTATION_0
                    }

                    val config = DisplayConfig(
                        width = currentSettings.resolutionWidth,
                        height = currentSettings.resolutionHeight,
                        showUnit = currentSettings.showUnit,
                        brightness = currentSettings.brightness,
                        colorMode = colorMode,
                        orientation = orientation
                    )

                    val frame: PixelFrame = renderer.render(displaySpeed, config)
                    val packetsRes = encoder.encodeFrame(frame)

                    packetsRes.onSuccess { packets ->
                        for (packet in packets) {
                            transport.send(packet)
                        }
                        lastSentSpeed = displaySpeed
                        DebugLogger.d("Service", "Rendered & sent frame for speed $displaySpeed km/h")
                    }.onFailure { err ->
                        DebugLogger.e("Service", "Frame encoding failed: ${err.localizedMessage}")
                    }
                }

                delay(currentSettings.updateIntervalMs)
            }
        }
    }

    private fun stopRenderLoop() {
        renderLoopJob?.cancel()
        renderLoopJob = null
        lastSentSpeed = null
    }

    private fun scheduleAutoReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = serviceScope.launch {
            var attempt = 1
            var delayMs = 1000L
            while (_isServiceRunning.value && connectionState.value !is BluetoothConnectionState.Connected) {
                DebugLogger.w("Service", "Auto-reconnect attempt #$attempt (waiting ${delayMs / 1000}s)...")
                _connectionState.value = BluetoothConnectionState.Reconnecting(attempt)
                delay(delayMs)

                if (currentSettings.deviceAddress.isNotBlank()) {
                    transport.connect(currentSettings.deviceAddress)
                }

                attempt++
                delayMs = (delayMs * 2).coerceAtMost(30000L) // Exponential backoff up to 30s
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Divoom Speed Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time GPS speed and Divoom connection status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification() {
        if (!_isServiceRunning.value) return
        val speedStr = _currentSpeed.value.displayText
        val connStr = when (val state = _connectionState.value) {
            is BluetoothConnectionState.Connected -> "Connected (${currentSettings.deviceName})"
            is BluetoothConnectionState.Connecting -> "Connecting..."
            is BluetoothConnectionState.Reconnecting -> "Reconnecting (Attempt #${state.attempt})..."
            is BluetoothConnectionState.Error -> "Error: ${state.message}"
            else -> "Disconnected"
        }
        val notification = buildNotification("Speed: $speedStr km/h", connStr)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SpeedDisplayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop Service", stopPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
