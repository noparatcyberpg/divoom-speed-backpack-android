package com.divoomspeed.backpack.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.divoomspeed.backpack.bluetooth.AutoDetectDivoomTransport
import com.divoomspeed.backpack.bluetooth.BluetoothConnectionState
import com.divoomspeed.backpack.bluetooth.DeviceCapabilities
import com.divoomspeed.backpack.bluetooth.DivoomTransport
import com.divoomspeed.backpack.bluetooth.FakeDivoomTransport
import com.divoomspeed.backpack.data.AppSettings
import com.divoomspeed.backpack.data.SettingsRepository
import com.divoomspeed.backpack.location.FakeSpeedTracker
import com.divoomspeed.backpack.location.SpeedReading
import com.divoomspeed.backpack.logging.DebugLogEntry
import com.divoomspeed.backpack.logging.DebugLogger
import com.divoomspeed.backpack.protocol.LegacyDivoomProtocolEncoder
import com.divoomspeed.backpack.renderer.DefaultSpeedImageRenderer
import com.divoomspeed.backpack.renderer.DisplayConfig
import com.divoomspeed.backpack.renderer.DisplayOrientation
import com.divoomspeed.backpack.renderer.PixelFrame
import com.divoomspeed.backpack.renderer.SpeedColorMode
import com.divoomspeed.backpack.renderer.SpeedImageRenderer
import com.divoomspeed.backpack.service.SpeedDisplayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    val settingsRepository = SettingsRepository(context)

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AppSettings()
    )

    val logs: StateFlow<List<DebugLogEntry>> = DebugLogger.logs

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _currentSpeedReading = MutableStateFlow(SpeedReading())
    val currentSpeedReading: StateFlow<SpeedReading> = _currentSpeedReading.asStateFlow()

    private val _previewFrame = MutableStateFlow(PixelFrame(16, 16, IntArray(256)))
    val previewFrame: StateFlow<PixelFrame> = _previewFrame.asStateFlow()

    private val _deviceCapabilities = MutableStateFlow<DeviceCapabilities?>(null)
    val deviceCapabilities: StateFlow<DeviceCapabilities?> = _deviceCapabilities.asStateFlow()

    private val renderer: SpeedImageRenderer = DefaultSpeedImageRenderer()
    private val fakeTracker = FakeSpeedTracker()
    private var transport: DivoomTransport = AutoDetectDivoomTransport(context)
    private val encoder = LegacyDivoomProtocolEncoder()

    init {
        // Update pixel preview whenever speed or settings change
        viewModelScope.launch {
            appSettings.collect { settings ->
                updatePreview(currentSpeedReading.value.displaySpeedKmh, settings)
            }
        }
        viewModelScope.launch {
            currentSpeedReading.collect { reading ->
                updatePreview(reading.displaySpeedKmh, appSettings.value)
            }
        }
    }

    fun updatePreview(speedKmh: Int, settings: AppSettings) {
        val colorMode = try {
            SpeedColorMode.valueOf(settings.colorMode)
        } catch (e: Exception) {
            SpeedColorMode.SPEED_BASED
        }
        val orientation = when (settings.rotationDegrees) {
            90 -> DisplayOrientation.ROTATION_90
            180 -> DisplayOrientation.ROTATION_180
            270 -> DisplayOrientation.ROTATION_270
            else -> DisplayOrientation.ROTATION_0
        }
        val config = DisplayConfig(
            width = settings.resolutionWidth,
            height = settings.resolutionHeight,
            showUnit = settings.showUnit,
            brightness = settings.brightness,
            colorMode = colorMode,
            orientation = orientation
        )
        _previewFrame.value = renderer.render(speedKmh, config)
    }

    fun startSpeedService() {
        val intent = Intent(context, SpeedDisplayService::class.java).apply {
            action = SpeedDisplayService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopSpeedService() {
        val intent = Intent(context, SpeedDisplayService::class.java).apply {
            action = SpeedDisplayService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun connectDevice() {
        val address = appSettings.value.deviceAddress
        if (address.isBlank()) {
            DebugLogger.w("UI", "No device address saved")
            return
        }
        viewModelScope.launch {
            _connectionState.value = BluetoothConnectionState.Connecting
            val res = transport.connect(address)
            if (res.isSuccess) {
                _connectionState.value = BluetoothConnectionState.Connected
            } else {
                _connectionState.value = BluetoothConnectionState.Error(res.exceptionOrNull()?.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            transport.disconnect()
            _connectionState.value = BluetoothConnectionState.Idle
        }
    }

    fun saveSelectedDevice(address: String, name: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedDevice(address, name)
            inspectDevice(address)
        }
    }

    fun inspectDevice(address: String) {
        viewModelScope.launch {
            val res = transport.discoverDeviceCapabilities(address)
            res.onSuccess { caps ->
                _deviceCapabilities.value = caps
            }.onFailure { err ->
                DebugLogger.e("UI", "Inspect device failed: ${err.localizedMessage}")
            }
        }
    }

    fun sendTestColor(color: Int) {
        viewModelScope.launch {
            val pixels = IntArray(256) { color }
            val frame = PixelFrame(16, 16, pixels)
            sendFrameToDevice(frame)
        }
    }

    fun sendTestSpeedNumber(speedKmh: Int) {
        viewModelScope.launch {
            val config = DisplayConfig(
                width = appSettings.value.resolutionWidth,
                height = appSettings.value.resolutionHeight,
                showUnit = appSettings.value.showUnit
            )
            val frame = renderer.render(speedKmh, config)
            sendFrameToDevice(frame)
        }
    }

    fun sendClearScreen() {
        viewModelScope.launch {
            encoder.encodeClearScreen().onSuccess { packets ->
                for (packet in packets) {
                    transport.send(packet)
                }
            }
        }
    }

    fun sendRawHexPacket(hexStr: String) {
        viewModelScope.launch {
            try {
                val cleanHex = hexStr.replace(" ", "")
                val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                transport.send(bytes)
            } catch (e: Exception) {
                DebugLogger.e("UI", "Invalid HEX string: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun sendFrameToDevice(frame: PixelFrame) {
        val packetsRes = encoder.encodeFrame(frame)
        packetsRes.onSuccess { packets ->
            for (p in packets) {
                transport.send(p)
            }
            DebugLogger.i("UI", "Test packet sent successfully")
        }.onFailure { err ->
            DebugLogger.e("UI", "Failed to encode test frame: ${err.localizedMessage}")
        }
    }

    fun startDemoMode() {
        viewModelScope.launch {
            settingsRepository.updateDemoMode(true)
            transport = FakeDivoomTransport()
            fakeTracker.start()
            fakeTracker.startAutoDemo(viewModelScope)
            viewModelScope.launch {
                fakeTracker.speedState.collect {
                    _currentSpeedReading.value = it
                }
            }
        }
    }

    fun stopDemoMode() {
        viewModelScope.launch {
            settingsRepository.updateDemoMode(false)
            fakeTracker.stop()
            transport = AutoDetectDivoomTransport(context)
        }
    }
}
