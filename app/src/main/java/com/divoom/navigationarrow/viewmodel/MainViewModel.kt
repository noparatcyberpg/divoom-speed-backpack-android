package com.divoom.navigationarrow.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.model.DisplayState
import com.divoom.navigationarrow.domain.model.LogEntry
import com.divoom.navigationarrow.domain.model.LogLevel
import com.divoom.navigationarrow.domain.model.PixelFrame
import com.divoom.navigationarrow.domain.model.StoppedCartoons
import com.divoom.navigationarrow.domain.model.TestPatterns
import com.divoom.navigationarrow.domain.model.TransportDiagnostics
import com.divoom.navigationarrow.domain.protocol.ModernBackpackBleProtocol
import com.divoom.navigationarrow.domain.protocol.PacketEnvelope
import com.divoom.navigationarrow.domain.protocol.ProtocolUuids
import com.divoom.navigationarrow.domain.transport.BleDivoomTransport
import com.divoom.navigationarrow.domain.transport.WriteType
import com.divoom.navigationarrow.navigation.GoogleMapsNotificationListener
import com.divoom.navigationarrow.navigation.MotionState
import com.divoom.navigationarrow.navigation.MotionStateBus
import com.divoom.navigationarrow.navigation.NavigationInstruction
import com.divoom.navigationarrow.navigation.NavigationInstructionBus
import com.divoom.navigationarrow.navigation.NavigationManeuver
import com.divoom.navigationarrow.navigation.NavigationForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PairedDeviceInfo(
    val name: String,
    val address: String,
    val deviceType: String,
    val bondState: String,
    val rssi: Int? = null,
    val isPaired: Boolean = false
)

private const val MAX_BACKGROUND_RECONNECT_ATTEMPTS = 6

internal fun animationDelayMillis(
    speedKmh: Int,
    hasFix: Boolean,
    isStopped: Boolean
): Long = when {
    isStopped -> 420L
    !hasFix -> 900L
    speedKmh >= 80 -> 120L
    speedKmh >= 50 -> 200L
    speedKmh >= 25 -> 330L
    speedKmh >= 10 -> 500L
    else -> 700L
}

internal fun reconnectDelayMillis(attempt: Int): Long =
    when (attempt.coerceAtLeast(0)) {
        0 -> 1_000L
        1 -> 2_000L
        2 -> 4_000L
        3 -> 8_000L
        4 -> 15_000L
        else -> 30_000L
    }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? =
        application.getSystemService(BluetoothManager::class.java)?.adapter

    private val logFlow = MutableSharedFlow<LogEntry>(replay = 80, extraBufferCapacity = 64)
    val logs: SharedFlow<LogEntry> = logFlow.asSharedFlow()

    private val _logList = MutableStateFlow<List<LogEntry>>(emptyList())
    val logList: StateFlow<List<LogEntry>> = _logList.asStateFlow()

    private val controllerUuid: String = application
        .getSharedPreferences("divoom_arrow", Context.MODE_PRIVATE)
        .let { preferences ->
            preferences.getString("controller_uuid", null)
                ?: UUID.randomUUID().toString().also {
                    preferences.edit().putString("controller_uuid", it).apply()
                }
        }
    private val bleProtocol = ModernBackpackBleProtocol(controllerUuid)
    private val bleTransport = BleDivoomTransport(application, logFlow, bleProtocol)
    private val protocol get() = bleProtocol

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _diagnostics = MutableStateFlow(TransportDiagnostics())
    val diagnostics: StateFlow<TransportDiagnostics> = _diagnostics.asStateFlow()

    private val _displayState = MutableStateFlow(DisplayState())
    val displayState: StateFlow<DisplayState> = _displayState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _deviceDiscoveryError = MutableStateFlow<String?>(null)
    val deviceDiscoveryError: StateFlow<String?> = _deviceDiscoveryError.asStateFlow()

    private val _selectedAddress = MutableStateFlow<String?>(null)
    val selectedAddress: StateFlow<String?> = _selectedAddress.asStateFlow()

    private val _debugMode = MutableStateFlow(false)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    private val _advancedMode = MutableStateFlow(false)
    val advancedMode: StateFlow<Boolean> = _advancedMode.asStateFlow()

    private val _brightness = MutableStateFlow(100)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _hexInput = MutableStateFlow("")
    val hexInput: StateFlow<String> = _hexInput.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notificationAccessGranted = MutableStateFlow(false)
    val notificationAccessGranted: StateFlow<Boolean> =
        _notificationAccessGranted.asStateFlow()

    private val _navigationModeEnabled = MutableStateFlow(false)
    val navigationModeEnabled: StateFlow<Boolean> = _navigationModeEnabled.asStateFlow()

    private val _lastNavigationInstruction = MutableStateFlow<NavigationInstruction?>(null)
    val lastNavigationInstruction: StateFlow<NavigationInstruction?> =
        _lastNavigationInstruction.asStateFlow()

    private val _motionPermissionGranted = MutableStateFlow(false)
    val motionPermissionGranted: StateFlow<Boolean> =
        _motionPermissionGranted.asStateFlow()

    private val _motionState = MutableStateFlow(MotionState())
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private var lastAutoSentManeuver: NavigationManeuver? = null
    private var showingDistantTurnWaitingAnimation = false
    private var navigationAnimationJob: Job? = null
    private var manualAnimationJob: Job? = null
    private var scanCallback: ScanCallback? = null
    private var scanStopJob: Job? = null
    private var reconnectJob: Job? = null
    private var shouldMaintainConnection = false
    private var reconnectAttempt = 0

    private val _reconnectMessage = MutableStateFlow<String?>(null)
    val reconnectMessage: StateFlow<String?> = _reconnectMessage.asStateFlow()
    private val deviceComparator =
        compareByDescending<PairedDeviceInfo> { isLikelyDivoomName(it.name) }
            .thenByDescending { it.rssi ?: Int.MIN_VALUE }
            .thenBy { it.name.lowercase() }

    init {
        viewModelScope.launch {
            logs.collect { entry ->
                _logList.value = (_logList.value + entry).takeLast(400)
            }
        }
        viewModelScope.launch {
            bleTransport.connectionState.collect { state ->
                _connectionState.value = state
                if (
                    shouldMaintainConnection &&
                    (state is ConnectionState.Disconnected || state is ConnectionState.Error)
                ) {
                    scheduleReconnect()
                }
            }
        }
        viewModelScope.launch {
            bleTransport.diagnostics.collect { _diagnostics.value = it }
        }
        viewModelScope.launch {
            NavigationInstructionBus.latest.collect { instruction ->
                if (instruction == null) return@collect
                _lastNavigationInstruction.value = instruction
                if (_navigationModeEnabled.value) {
                    sendNavigationInstruction(instruction)
                }
            }
        }
        viewModelScope.launch {
            MotionStateBus.state.collect { state ->
                _motionState.value = state
            }
        }
        refreshPairedDevices()
        refreshNavigationAccess()
        refreshMotionPermission()
        log(
            LogLevel.INFO,
            "VM",
            "Protocol: ${protocol.name}. UUIDs from reference — write=${ProtocolUuids.WRITE_CHAR_STRING}"
        )
    }

    fun setDebugMode(enabled: Boolean) {
        _debugMode.value = enabled
    }

    fun setAdvancedMode(enabled: Boolean) {
        _advancedMode.value = enabled
        if (!enabled) bleTransport.clearAdvancedCharacteristic()
    }

    fun setBrightness(value: Int) {
        _brightness.value = value.coerceIn(0, 100)
    }

    fun refreshNavigationAccess() {
        val context = getApplication<Application>()
        val granted = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
        _notificationAccessGranted.value = granted
        if (!granted && _navigationModeEnabled.value) {
            disableNavigationMode()
        }
    }

    fun refreshMotionPermission() {
        val context = getApplication<Application>()
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        _motionPermissionGranted.value = granted
        if (!granted && _navigationModeEnabled.value) {
            disableNavigationMode()
        }
    }

    fun openNavigationAccessSettings() {
        val context = getApplication<Application>()
        val detailIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(
                            context,
                            GoogleMapsNotificationListener::class.java
                        ).flattenToString()
                    )
                }
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
        try {
            context.startActivity(
                detailIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun openGoogleMaps() {
        val context = getApplication<Application>()
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")).apply {
                    setPackage("com.google.android.apps.maps")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            Toast.makeText(context, "ไม่พบแอป Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    fun setNavigationModeEnabled(enabled: Boolean) {
        refreshNavigationAccess()
        refreshMotionPermission()
        if (enabled && !_notificationAccessGranted.value) {
            openNavigationAccessSettings()
            return
        }
        if (enabled && !_motionPermissionGranted.value) {
            _displayState.value = _displayState.value.copy(
                lastError = "อนุญาตการเคลื่อนไหวเพื่อปรับจังหวะแอนิเมชันก่อน"
            )
            return
        }
        if (enabled && preflight() != null) {
            _displayState.value = _displayState.value.copy(
                lastError = "เชื่อมต่อกระเป๋าให้พร้อมก่อนเปิดโหมดนำทาง"
            )
            return
        }
        if (!enabled) {
            disableNavigationMode()
            return
        }
        _navigationModeEnabled.value = enabled
        lastAutoSentManeuver = null
        showingDistantTurnWaitingAnimation = false
        try {
            NavigationForegroundService.start(getApplication())
        } catch (_: Exception) {
            _navigationModeEnabled.value = false
            _displayState.value = _displayState.value.copy(
                lastError = "เริ่มโหมดนำทางไม่สำเร็จ กรุณาลองใหม่"
            )
            return
        }
        val recentInstruction = _lastNavigationInstruction.value
            ?.takeIf { System.currentTimeMillis() - it.receivedAt < 60_000 }
        if (recentInstruction != null) {
            sendNavigationInstruction(recentInstruction)
        } else {
            startContinuousNavigationAnimation(
                label = "รอคำสั่ง Google Maps",
                baseFrame = TestPatterns.getWaitingHeart()
            )
        }
    }

    private fun sendNavigationInstruction(instruction: NavigationInstruction) {
        if (!_navigationModeEnabled.value) return
        if (_busy.value || preflight() != null) return
        if (!instruction.shouldDisplayTurnArrow()) {
            lastAutoSentManeuver = null
            if (
                !showingDistantTurnWaitingAnimation ||
                navigationAnimationJob?.isActive != true
            ) {
                showingDistantTurnWaitingAnimation = true
                startContinuousNavigationAnimation(
                    label = "รอลูกศรเลี้ยวในระยะ 100 เมตร",
                    baseFrame = TestPatterns.getWaitingHeart()
                )
            }
            log(
                LogLevel.INFO,
                "NAV",
                "รอให้เหลือไม่เกิน 100 เมตรก่อนแสดงลูกศรเลี้ยว"
            )
            return
        }
        if (
            instruction.maneuver == lastAutoSentManeuver &&
            navigationAnimationJob?.isActive == true
        ) {
            return
        }
        val baseFrame = frameForPattern(instruction.maneuver.patternName) ?: return
        showingDistantTurnWaitingAnimation = false
        lastAutoSentManeuver = instruction.maneuver
        log(
            LogLevel.INFO,
            "NAV",
            "Google Maps: ${instruction.maneuver.name} — ${instruction.text}"
        )
        startContinuousNavigationAnimation(
            label = "Google Maps ${instruction.maneuver.name}",
            baseFrame = baseFrame
        )
    }

    private fun startContinuousNavigationAnimation(label: String, baseFrame: PixelFrame) {
        navigationAnimationJob?.cancel()
        navigationAnimationJob = viewModelScope.launch {
            val symbolFrames = TestPatterns.getCuteAnimationFrames(baseFrame)
            var symbolIndex = 0
            var cartoonFrameIndex = 0
            var cartoonLoops = 0
            var cartoonIndex: Int? = null
            var showingStoppedCartoon = false
            while (_navigationModeEnabled.value) {
                val motion = _motionState.value
                if (motion.isStopped != showingStoppedCartoon) {
                    showingStoppedCartoon = motion.isStopped
                    symbolIndex = 0
                    cartoonFrameIndex = 0
                    cartoonLoops = 0
                    cartoonIndex =
                        if (showingStoppedCartoon) {
                            StoppedCartoons.nextIndex(current = cartoonIndex)
                        } else {
                            null
                        }
                }
                val frame = if (showingStoppedCartoon) {
                    var cartoon = StoppedCartoons.all[cartoonIndex ?: 0]
                    if (cartoonFrameIndex >= cartoon.frames.size) {
                        cartoonFrameIndex = 0
                        cartoonLoops++
                        if (cartoonLoops >= 3) {
                            cartoonIndex = StoppedCartoons.nextIndex(current = cartoonIndex)
                            cartoon = StoppedCartoons.all[cartoonIndex]
                            cartoonLoops = 0
                        }
                    }
                    cartoon.frames[cartoonFrameIndex].also { cartoonFrameIndex++ }
                } else {
                    symbolFrames[symbolIndex % symbolFrames.size].also { symbolIndex++ }
                }
                if (!waitForDisplayIdle()) return@launch
                sendFrame(
                    if (showingStoppedCartoon) {
                        "$label • stopped cartoon"
                    } else {
                        "$label • animation"
                    },
                    frame
                )
                if (!waitForDisplayIdle()) return@launch
                delay(
                    animationDelayMillis(
                        speedKmh = motion.speedKmh,
                        hasFix = motion.hasFix,
                        isStopped = motion.isStopped
                    )
                )
            }
        }
    }

    private fun startShortSymbolAnimation(label: String, baseFrame: PixelFrame) {
        manualAnimationJob?.cancel()
        manualAnimationJob = viewModelScope.launch {
            val frames = TestPatterns.getCuteAnimationFrames(baseFrame)
            for ((index, frame) in frames.withIndex()) {
                if (!waitForDisplayIdle()) return@launch
                sendFrame("$label • ${index + 1}/${frames.size}", frame)
                if (!waitForDisplayIdle()) return@launch
                if (index < frames.lastIndex) delay(140)
            }
        }
    }

    private suspend fun waitForDisplayIdle(timeoutMs: Long = 8_000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (_busy.value) {
            if (System.currentTimeMillis() >= deadline) return false
            delay(40)
        }
        return true
    }

    private fun disableNavigationMode() {
        _navigationModeEnabled.value = false
        navigationAnimationJob?.cancel()
        navigationAnimationJob = null
        lastAutoSentManeuver = null
        showingDistantTurnWaitingAnimation = false
        NavigationForegroundService.stop(getApplication())
    }

    fun selectDevice(address: String) {
        _selectedAddress.value = address
        log(LogLevel.INFO, "VM", "Selected device $address")
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _deviceDiscoveryError.value = "กรุณาเปิด Bluetooth แล้วลองใหม่"
            log(LogLevel.WARN, "VM", "Bluetooth is turned off or unsupported")
            _pairedDevices.value = emptyList()
            return
        }
        val devices = try {
            adapter.bondedDevices
                .filter { isLikelyDivoomName(it.name.orEmpty()) }
                .map { dev -> dev.toDeviceInfo(isPaired = true) }
        } catch (_: SecurityException) {
            _deviceDiscoveryError.value = "กรุณาอนุญาตให้แอปค้นหาอุปกรณ์ Bluetooth"
            emptyList()
        }
        _pairedDevices.value = devices.sortedWith(deviceComparator)
        log(LogLevel.INFO, "VM", "Loaded ${devices.size} paired Bluetooth devices")
        selectBestDeviceIfNeeded()
    }

    @SuppressLint("MissingPermission")
    fun scanForDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _deviceDiscoveryError.value = "กรุณาเปิด Bluetooth แล้วลองใหม่"
            return
        }
        stopDeviceScan()
        refreshPairedDevices()
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _deviceDiscoveryError.value = "โทรศัพท์เครื่องนี้ไม่รองรับการค้นหา Bluetooth LE"
            return
        }

        _deviceDiscoveryError.value = null
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                addScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach(::addScanResult)
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
                _deviceDiscoveryError.value = "ค้นหาอุปกรณ์ไม่สำเร็จ ลองปิด–เปิด Bluetooth แล้วค้นหาอีกครั้ง"
                log(LogLevel.ERROR, "VM", "BLE scan failed errorCode=$errorCode")
            }
        }
        scanCallback = callback
        try {
            scanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                callback
            )
            _isScanning.value = true
            log(LogLevel.INFO, "VM", "Scanning for nearby Divoom devices")
            scanStopJob = viewModelScope.launch {
                delay(8_000)
                stopDeviceScan()
            }
        } catch (_: SecurityException) {
            scanCallback = null
            _isScanning.value = false
            _deviceDiscoveryError.value = "กรุณาอนุญาตให้แอปค้นหาอุปกรณ์ Bluetooth"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDeviceScan() {
        scanStopJob?.cancel()
        scanStopJob = null
        val callback = scanCallback ?: run {
            _isScanning.value = false
            return
        }
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (_: SecurityException) {
        } finally {
            scanCallback = null
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun addScanResult(result: ScanResult) {
        val device = result.device
        val advertisedName = result.scanRecord?.deviceName
        val name = advertisedName ?: try {
            device.name
        } catch (_: SecurityException) {
            null
        }
        val advertisesDivoomService = result.scanRecord?.serviceUuids
            ?.any { it.uuid == ProtocolUuids.SERVICE } == true
        if (!advertisesDivoomService && !isLikelyDivoomName(name.orEmpty())) return

        val info = device.toDeviceInfo(
            isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
            nameOverride = name,
            rssi = result.rssi
        )
        val merged = _pairedDevices.value
            .filterNot { it.address == info.address } + info
        _pairedDevices.value = merged.sortedWith(deviceComparator)
        selectBestDeviceIfNeeded()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toDeviceInfo(
        isPaired: Boolean,
        nameOverride: String? = null,
        rssi: Int? = null
    ): PairedDeviceInfo = PairedDeviceInfo(
        name = nameOverride ?: name ?: "Divoom",
        address = address,
        deviceType = when (type) {
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "BLE + Classic"
            else -> "Bluetooth"
        },
        bondState = if (isPaired) "จับคู่แล้ว" else "พร้อมเชื่อมต่อ",
        rssi = rssi,
        isPaired = isPaired
    )

    private fun isLikelyDivoomName(name: String): Boolean =
        listOf("Divoom", "Backpack", "Pixoo", "Cyberbag", "TimeBox")
            .any { name.contains(it, ignoreCase = true) }

    private fun selectBestDeviceIfNeeded() {
        if (_selectedAddress.value != null) return
        (_pairedDevices.value.firstOrNull { isLikelyDivoomName(it.name) }
            ?: _pairedDevices.value.firstOrNull())
            ?.let { _selectedAddress.value = it.address }
    }

    fun connectDevice(address: String? = _selectedAddress.value) {
        val addr = address ?: run {
            log(LogLevel.ERROR, "VM", "No device address selected")
            return
        }
        _selectedAddress.value = addr
        shouldMaintainConnection = true
        reconnectAttempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
        _reconnectMessage.value = null
        stopDeviceScan()
        viewModelScope.launch {
            if (!connectAndInitialize(addr, automatic = false)) {
                scheduleReconnect()
            }
        }
    }

    private suspend fun connectAndInitialize(
        address: String,
        automatic: Boolean
    ): Boolean {
        if (!shouldMaintainConnection) return false
        _busy.value = true
        var ready = false
        try {
            log(
                LogLevel.INFO,
                "VM",
                if (automatic) {
                    "Automatic reconnect attempt ${reconnectAttempt + 1}"
                } else if (_debugMode.value) {
                    "Debug Mode: Connect only (Initialize separately)"
                } else {
                    "Normal Mode: Connect → Discover → Initialize"
                }
            )
            val connected = bleTransport.connect(address)
            if (connected.isFailure) {
                log(
                    LogLevel.ERROR,
                    "VM",
                    connected.exceptionOrNull()?.message ?: "GATT not connected"
                )
                return false
            }
            if (_debugMode.value && !automatic) {
                ready = true
                return true
            }

            val initialized = bleTransport.initializeSession()
            if (initialized.isFailure) {
                log(
                    LogLevel.ERROR,
                    "VM",
                    "Session initialization failed: ${initialized.exceptionOrNull()?.message}"
                )
                return false
            }
            ready = true
            reconnectAttempt = 0
            _reconnectMessage.value = null
            log(
                LogLevel.INFO,
                "VM",
                if (automatic) "Connected again automatically" else "Connection ready"
            )
            return true
        } finally {
            _busy.value = false
            if (ready && automatic && _navigationModeEnabled.value) {
                lastAutoSentManeuver = null
                val instruction = _lastNavigationInstruction.value
                if (instruction != null) {
                    sendNavigationInstruction(instruction)
                } else {
                    startContinuousNavigationAnimation(
                        label = "รอคำสั่ง Google Maps",
                        baseFrame = TestPatterns.getWaitingHeart()
                    )
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!shouldMaintainConnection || reconnectJob?.isActive == true) return
        val address = _selectedAddress.value ?: return
        reconnectJob = viewModelScope.launch {
            while (shouldMaintainConnection) {
                if (
                    reconnectAttempt >= MAX_BACKGROUND_RECONNECT_ATTEMPTS &&
                    !_navigationModeEnabled.value
                ) {
                    _reconnectMessage.value = "เชื่อมต่อกลับไม่สำเร็จ กดเพื่อลองอีกครั้ง"
                    return@launch
                }

                val delayMs = reconnectDelayMillis(reconnectAttempt)
                val seconds = (delayMs / 1_000L).coerceAtLeast(1L)
                _reconnectMessage.value =
                    "สัญญาณหลุด • กำลังเชื่อมต่อใหม่ใน $seconds วินาที"
                log(
                    LogLevel.WARN,
                    "VM",
                    "Link lost; reconnecting in ${delayMs}ms (attempt ${reconnectAttempt + 1})"
                )
                delay(delayMs)
                if (!shouldMaintainConnection) return@launch
                if (_busy.value) {
                    delay(500)
                    continue
                }

                _reconnectMessage.value = "กำลังเชื่อมต่อกลับ…"
                val success = connectAndInitialize(address, automatic = true)
                if (success) return@launch
                reconnectAttempt++
            }
        }
    }

    fun initializeSession() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = bleTransport.initializeSession()
                if (res.isFailure) {
                    log(
                        LogLevel.ERROR,
                        "VM",
                        "Session initialization failed: ${res.exceptionOrNull()?.message}"
                    )
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun disconnectDevice() {
        shouldMaintainConnection = false
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
        _reconnectMessage.value = null
        disableNavigationMode()
        viewModelScope.launch {
            bleTransport.disconnect()
        }
    }

    fun discoverServices() {
        viewModelScope.launch {
            val res = bleTransport.discoverServicesNow()
            if (res.isFailure) {
                log(LogLevel.ERROR, "VM", res.exceptionOrNull()?.message ?: "Discover failed")
            }
        }
    }

    fun requestMtu() {
        viewModelScope.launch {
            val res = bleTransport.requestMtuNow(512)
            if (res.isFailure) {
                log(LogLevel.ERROR, "VM", res.exceptionOrNull()?.message ?: "MTU failed")
            } else {
                log(LogLevel.INFO, "VM", "MTU=${res.getOrNull()}")
            }
        }
    }

    fun selectBleWriteChar(serviceUuid: String, charUuid: String) {
        try {
            bleTransport.setAdvancedCharacteristic(
                UUID.fromString(serviceUuid),
                UUID.fromString(charUuid)
            )
            log(LogLevel.INFO, "VM", "Advanced Mode write char=$charUuid")
        } catch (e: Exception) {
            log(LogLevel.ERROR, "VM", "Invalid UUID: ${e.message}")
        }
    }

    fun updateHexInput(input: String) {
        _hexInput.value = input
    }

    fun sendHexPacket() {
        val hexStr = _hexInput.value.replace(" ", "").replace("\n", "").trim()
        if (hexStr.isEmpty() || hexStr.length % 2 != 0) {
            log(LogLevel.ERROR, "VM", "Invalid HEX string length")
            return
        }
        viewModelScope.launch {
            try {
                val bytes = hexStr.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val res = bleTransport.writePacket(bytes, WriteType.NO_RESPONSE)
                if (res.isFailure) {
                    log(LogLevel.ERROR, "HEX", res.exceptionOrNull()?.message ?: "Write failed")
                } else {
                    log(LogLevel.TX, "HEX", "Wrote ${bytes.size} bytes")
                }
            } catch (e: Exception) {
                log(LogLevel.ERROR, "HEX", "Parse error: ${e.message}")
            }
        }
    }

    fun sendBrightness() {
        viewModelScope.launch {
            val gate = preflight()
            if (gate != null) {
                _displayState.value = _displayState.value.copy(
                    lastError = gate,
                    lastSendResult = gate
                )
                return@launch
            }
            _busy.value = true
            try {
                val encoded = protocol.encodeBrightness(_brightness.value)
                if (encoded.isFailure) {
                    log(LogLevel.ERROR, "VM", encoded.exceptionOrNull()?.message ?: "encode failed")
                    return@launch
                }
                for (pkt in encoded.getOrThrow()) {
                    val r = bleTransport.writePacket(pkt, WriteType.NO_RESPONSE)
                    if (r.isFailure) {
                        log(LogLevel.ERROR, "VM", r.exceptionOrNull()?.message ?: "write failed")
                        return@launch
                    }
                }
                log(LogLevel.INFO, "VM", "Brightness set to ${_brightness.value}")
                _displayState.value = _displayState.value.copy(
                    lastError = null,
                    lastSendResult = "ปรับความสว่างเป็น ${_brightness.value}% แล้ว"
                )
            } finally {
                _busy.value = false
            }
        }
    }

    fun sendTestPattern(patternName: String) {
        val frame = frameForPattern(patternName)
        if (frame == null) {
            log(LogLevel.ERROR, "DISPLAY", "Unknown pattern: $patternName")
            return
        }
        if (
            patternName in setOf(
                "Arrow Left",
                "Arrow Right",
                "Arrow Straight",
                "U-Turn",
                "Roundabout",
                "Destination"
            )
        ) {
            startShortSymbolAnimation(patternName, frame)
            return
        }
        sendFrame(
            label = patternName,
            frame = frame,
            useLightingCommand =
                patternName.startsWith("Solid ") && patternName != "Solid Black"
        )
    }

    private fun frameForPattern(patternName: String): PixelFrame? =
        when (patternName) {
            "Solid Red" -> TestPatterns.getSolidRed()
            "Solid Green" -> TestPatterns.getSolidGreen()
            "Solid Blue" -> TestPatterns.getSolidBlue()
            "Solid White" -> TestPatterns.getSolidWhite()
            "Solid Black", "Clear Screen" -> TestPatterns.getClear()
            "Checkerboard" -> TestPatterns.getCheckerboard()
            "Horizontal Gradient" -> TestPatterns.getHorizontalGradient()
            "Arrow Left" -> TestPatterns.getArrowLeft()
            "Arrow Right" -> TestPatterns.getArrowRight()
            "Arrow Straight" -> TestPatterns.getArrowStraight()
            "U-Turn" -> TestPatterns.getUTurn()
            "Roundabout" -> TestPatterns.getRoundabout()
            "Destination" -> TestPatterns.getDestination()
            "Send 50", "50" -> TestPatterns.getNumber50()
            else -> null
        }

    private fun sendFrame(
        label: String,
        frame: PixelFrame,
        useLightingCommand: Boolean = false
    ) {
        if (_busy.value) return
        _busy.value = true
        _displayState.value = _displayState.value.copy(
            pendingFrame = frame,
            lastError = null,
            lastSendResult = null
        )
        viewModelScope.launch {
            try {
                val gate = preflight()
                if (gate != null) {
                    _displayState.value = _displayState.value.copy(
                        pendingFrame = null,
                        lastError = gate,
                        lastSendResult = gate
                    )
                    log(LogLevel.ERROR, "DISPLAY", gate)
                    return@launch
                }

                // Solid colors: lighting opcode (iOS path) + also keep PixelFrame for preview
                val packetsResult = if (useLightingCommand) {
                    val rgb = frame.getPixel(0, 0)
                    protocol.encodeLighting(
                        (rgb shr 16) and 0xFF,
                        (rgb shr 8) and 0xFF,
                        rgb and 0xFF,
                        _brightness.value
                    )
                } else {
                    protocol.encodeStaticImage(frame)
                }

                if (packetsResult.isFailure) {
                    val err = packetsResult.exceptionOrNull()?.message ?: "Packet encoding failed"
                    _displayState.value = _displayState.value.copy(
                        pendingFrame = null,
                        lastError = err,
                        lastSendResult = err
                    )
                    log(LogLevel.ERROR, "DISPLAY", err)
                    return@launch
                }

                val packets = packetsResult.getOrThrow()
                var bytesSent = 0
                for ((idx, packet) in packets.withIndex()) {
                    log(
                        LogLevel.TX,
                        "DISPLAY",
                        "Sending packet #$idx/${packets.size} (${packet.size} B)",
                        hex = PacketEnvelope.toHex(packet)
                    )
                    val res = bleTransport.writePacket(packet, WriteType.NO_RESPONSE)
                    if (res.isFailure) {
                        val err = res.exceptionOrNull()?.message
                            ?: "Device disconnected during transfer"
                        _displayState.value = _displayState.value.copy(
                            pendingFrame = null,
                            lastError = err,
                            lastSendResult = err
                        )
                        log(LogLevel.ERROR, "DISPLAY", "Packet #$idx failed: $err")
                        return@launch
                    }
                    bytesSent += packet.size
                }

                val now = System.currentTimeMillis()
                _displayState.value = DisplayState(
                    pendingFrame = null,
                    lastSuccessfullySentFrame = frame,
                    lastSentAt = now,
                    lastSendResult = "OK: $label (${packets.size} packets, $bytesSent bytes)",
                    lastError = null,
                    lastPacketCount = packets.size,
                    lastBytesSent = bytesSent
                )
                log(LogLevel.INFO, "DISPLAY", "Send OK: $label")
            } finally {
                _busy.value = false
            }
        }
    }

    private fun preflight(): String? {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) return "ยังไม่ได้เชื่อมต่อกับกระเป๋า"
        val d = _diagnostics.value
        if (!d.servicesDiscovered) return "ยังเตรียมการเชื่อมต่อไม่เสร็จ"
        if (d.characteristicUuid == null) return "กระเป๋ารุ่นนี้ยังไม่รองรับ"
        if (!bleTransport.isSessionInitialized()) return "ยังเตรียมหน้าจอไม่เสร็จ กรุณาเชื่อมต่อใหม่"
        return null
    }

    override fun onCleared() {
        shouldMaintainConnection = false
        reconnectJob?.cancel()
        navigationAnimationJob?.cancel()
        manualAnimationJob?.cancel()
        stopDeviceScan()
        NavigationForegroundService.stop(getApplication())
        super.onCleared()
    }

    fun copyDeviceReport() {
        val report = bleTransport.buildDeviceReport()
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Divoom Device Report", report))
        Toast.makeText(getApplication(), "Device report copied", Toast.LENGTH_SHORT).show()
    }

    fun exportDeviceReport() {
        val report = bleTransport.buildDeviceReport()
        val context = getApplication<Application>()
        val file = File(context.cacheDir, "divoom_device_report.txt")
        file.writeText(report)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Divoom Device Report")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Export Device Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun copyLogsToClipboard() {
        val context = getApplication<Application>()
        val logText = _logList.value.joinToString("\n") { it.toFormattedString() }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Divoom Logs", logText))
        Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
    }

    fun exportLogs() {
        val context = getApplication<Application>()
        val logText = _logList.value.joinToString("\n") { it.toFormattedString() }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Divoom BLE Logs")
            putExtra(Intent.EXTRA_TEXT, logText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Export Logs")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun clearLogs() {
        _logList.value = emptyList()
        bleTransport.clearHexLog()
        log(LogLevel.INFO, "VM", "Cleared log viewer")
    }

    private fun log(level: LogLevel, tag: String, msg: String, hex: String? = null) {
        android.util.Log.d(tag, "[$level] $msg")
        viewModelScope.launch {
            logFlow.emit(LogEntry(tag = tag, message = msg, hexData = hex, level = level))
        }
    }
}
