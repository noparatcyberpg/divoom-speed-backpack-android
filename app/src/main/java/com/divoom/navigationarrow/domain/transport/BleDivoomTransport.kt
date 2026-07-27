package com.divoom.navigationarrow.domain.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.model.ConnectionType
import com.divoom.navigationarrow.domain.model.GattCharacteristicInfo
import com.divoom.navigationarrow.domain.model.LogEntry
import com.divoom.navigationarrow.domain.model.LogLevel
import com.divoom.navigationarrow.domain.model.TransportDiagnostics
import com.divoom.navigationarrow.domain.protocol.ModernBackpackBleProtocol
import com.divoom.navigationarrow.domain.protocol.PacketEnvelope
import com.divoom.navigationarrow.domain.protocol.ProtocolUuids
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("MissingPermission")
class BleDivoomTransport(
    private val context: Context,
    private val logFlow: MutableSharedFlow<LogEntry>,
    private val protocol: ModernBackpackBleProtocol = ModernBackpackBleProtocol()
) : DivoomTransport {

    private val bluetoothAdapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _diagnostics = MutableStateFlow(TransportDiagnostics())
    override val diagnostics: StateFlow<TransportDiagnostics> = _diagnostics.asStateFlow()

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var currentMtu: Int = 23
    private var servicesDiscovered: Boolean = false
    private var sessionInitialized: Boolean = false
    private var targetAddress: String? = null
    private var advancedModeManualChar: Pair<UUID, UUID>? = null
    @Volatile private var notificationSetupPending: Boolean = false
    @Volatile private var intentionalDisconnect: Boolean = false

    private val gattChars = CopyOnWriteArrayList<GattCharacteristicInfo>()
    private val debugHexLog = ArrayDeque<String>()
    private val maxHexLogLines = 200

    private var connectDeferred: CompletableDeferred<Result<Unit>>? = null
    private var discoverDeferred: CompletableDeferred<Result<Unit>>? = null
    private var mtuDeferred: CompletableDeferred<Int>? = null
    private val writeAck = AtomicReference<CompletableDeferred<Int>?>(null)

    fun getProtocol(): ModernBackpackBleProtocol = protocol

    fun isSessionInitialized(): Boolean = sessionInitialized

    fun isServicesDiscovered(): Boolean = servicesDiscovered

    fun getGattCharacteristicInfos(): List<GattCharacteristicInfo> = gattChars.toList()

    fun setAdvancedCharacteristic(serviceUuid: UUID, charUuid: UUID) {
        advancedModeManualChar = serviceUuid to charUuid
        resolveWriteCharacteristic(bluetoothGatt)?.let {
            writeCharacteristic = it
            updateDiag { copy(characteristicUuid = it.uuid.toString(), serviceUuid = serviceUuid.toString()) }
            log(LogLevel.INFO, "BLE", "Advanced Mode selected write char: $charUuid")
        }
    }

    fun clearAdvancedCharacteristic() {
        advancedModeManualChar = null
        resolveWriteCharacteristic(bluetoothGatt)?.let {
            writeCharacteristic = it
            updateDiag { copy(characteristicUuid = it.uuid.toString()) }
        }
    }

    fun buildDeviceReport(): String {
        val d = _diagnostics.value
        val sb = StringBuilder()
        sb.appendLine("=== Divoom Device Report ===")
        sb.appendLine("Name: ${d.deviceName}")
        sb.appendLine("MAC: ${d.deviceAddress}")
        sb.appendLine("Type: ${d.deviceType}")
        sb.appendLine("Bond: ${d.bondState}")
        sb.appendLine("Connected: ${d.connected}")
        sb.appendLine("Services discovered: ${d.servicesDiscovered}")
        sb.appendLine("Session initialized: ${d.sessionInitialized}")
        sb.appendLine("RSSI: ${d.rssi}")
        sb.appendLine("MTU: ${d.mtu}")
        sb.appendLine("Service UUID: ${d.serviceUuid}")
        sb.appendLine("Write Characteristic: ${d.characteristicUuid}")
        sb.appendLine("Write type: ${d.writeType}")
        sb.appendLine("Notifications: ${d.notificationsEnabled}")
        sb.appendLine("Packets sent/failed: ${d.packetsSent}/${d.packetsFailed}")
        sb.appendLine("Last packet HEX: ${d.lastPacketHex}")
        sb.appendLine("Last notification HEX: ${d.lastNotificationHex}")
        sb.appendLine("Last GATT status: ${d.lastGattStatus}")
        sb.appendLine("Last error: ${d.lastError}")
        sb.appendLine()
        sb.appendLine("--- GATT ---")
        sb.appendLine(d.gattReport ?: "(none)")
        sb.appendLine()
        sb.appendLine("--- Recent HEX log ---")
        synchronized(debugHexLog) {
            debugHexLog.forEach { sb.appendLine(it) }
        }
        return sb.toString()
    }

    override suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        intentionalDisconnect = false
        targetAddress = address
        currentMtu = 23
        sessionInitialized = false
        servicesDiscovered = false
        notificationSetupPending = false
        protocol.resetSession()
        updateDiag {
            copy(
                deviceAddress = address,
                connected = false,
                servicesDiscovered = false,
                sessionInitialized = false,
                serviceUuid = null,
                characteristicUuid = null,
                mtu = currentMtu,
                notificationsEnabled = false,
                lastError = null
            )
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val err = "Bluetooth is disabled or not supported"
            log(LogLevel.ERROR, "BLE", err)
            _connectionState.value = ConnectionState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        // Close previous only on intentional reconnect
        val previousGatt = bluetoothGatt
        bluetoothGatt = null
        previousGatt?.let {
            try {
                it.disconnect()
                it.close()
            } catch (_: Exception) {
            }
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(address)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        val typeStr = when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Unknown"
        }
        val bondStr = when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            else -> "NONE"
        }

        updateDiag {
            copy(
                deviceName = device.name,
                deviceType = typeStr,
                bondState = bondStr
            )
        }

        _connectionState.value = ConnectionState.Connecting
        log(LogLevel.INFO, "BLE", "Connecting to ${device.name ?: "Unknown"} ($address) type=$typeStr bond=$bondStr")

        val deferred = CompletableDeferred<Result<Unit>>()
        connectDeferred = deferred
        discoverDeferred = CompletableDeferred()

        bluetoothGatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )

        try {
            withTimeout(20_000) { deferred.await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val err = "GATT connect timeout or failure: ${e.message}"
            log(LogLevel.ERROR, "BLE", err)
            updateDiag { copy(lastError = err) }
            connectDeferred = null
            val timedOutGatt = bluetoothGatt
            bluetoothGatt = null
            try {
                timedOutGatt?.disconnect()
                timedOutGatt?.close()
            } catch (_: Exception) {
            }
            _connectionState.value = ConnectionState.Error(err)
            Result.failure(IllegalStateException(err))
        }
    }

    override suspend fun initializeSession(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_connectionState.value !is ConnectionState.Connected) {
            val err = "GATT not connected"
            log(LogLevel.ERROR, "BLE", err)
            return@withContext Result.failure(IllegalStateException(err))
        }
        if (!servicesDiscovered) {
            val err = "Services not discovered"
            log(LogLevel.ERROR, "BLE", err)
            return@withContext Result.failure(IllegalStateException(err))
        }
        val char = writeCharacteristic
        if (char == null) {
            val err = "Write characteristic not found"
            log(LogLevel.ERROR, "BLE", err)
            return@withContext Result.failure(IllegalStateException(err))
        }
        if (!isWritable(char)) {
            val err = "Characteristic is not writable"
            log(LogLevel.ERROR, "BLE", err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        log(LogLevel.INFO, "BLE", "Starting Divoom session initialization (iOS-verbatim)...")
        protocol.resetSession()
        val packets = protocol.buildInitializationPackets()
        val pauseBefore = protocol.deviceInfoPacketIndex()

        for ((index, packet) in packets.withIndex()) {
            if (index == pauseBefore) {
                log(LogLevel.INFO, "BLE", "Init pause ${ProtocolUuids.INIT_DEVICE_INFO_PAUSE_MS}ms before deviceInfo")
                delay(ProtocolUuids.INIT_DEVICE_INFO_PAUSE_MS)
            }
            // Reference uses write-without-response for init/streaming
            val result = writePacket(packet, WriteType.NO_RESPONSE)
            if (result.isFailure) {
                val err = "Session initialization failed at packet #$index: ${result.exceptionOrNull()?.message}"
                log(LogLevel.ERROR, "BLE", err)
                sessionInitialized = false
                updateDiag { copy(sessionInitialized = false, lastError = err) }
                return@withContext Result.failure(IllegalStateException(err))
            }
            delay(ProtocolUuids.INIT_THROTTLE_MS)
        }

        sessionInitialized = true
        protocol.markInitialized()
        updateDiag { copy(sessionInitialized = true, lastError = null) }
        log(LogLevel.INFO, "BLE", "Init complete — display path unlocked")
        Result.success(Unit)
    }

    override suspend fun writePacket(packet: ByteArray, writeType: WriteType): Result<Unit> =
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                if (_connectionState.value !is ConnectionState.Connected) {
                    return@withContext failWrite("GATT not connected")
                }
                if (!servicesDiscovered) {
                    return@withContext failWrite("Services not discovered")
                }
                val gatt = bluetoothGatt ?: return@withContext failWrite("GATT not connected")
                val char = writeCharacteristic ?: return@withContext failWrite("Write characteristic not found")
                if (!isWritable(char)) {
                    return@withContext failWrite("Characteristic is not writable")
                }

                val chunks = PacketChunker.chunk(packet, ProtocolUuids.APP_CHUNK_SIZE)
                updateDiag {
                    copy(
                        packetsQueued = packetsQueued + chunks.size,
                        writeType = writeType.name,
                        lastPacketHex = PacketEnvelope.toHex(packet).take(512)
                    )
                }
                appendHexLog("PKT ${PacketEnvelope.toHex(packet)}")

                for ((i, chunk) in chunks.withIndex()) {
                    val chunkResult = writeSingleChunk(gatt, char, chunk, writeType)
                    if (chunkResult.isFailure) {
                        updateDiag {
                            copy(
                                packetsFailed = packetsFailed + 1,
                                lastError = chunkResult.exceptionOrNull()?.message
                            )
                        }
                        return@withContext chunkResult
                    }
                    log(
                        LogLevel.TX,
                        "BLE",
                        "Chunk ${i + 1}/${chunks.size} (${chunk.size} B) OK",
                        hexData = PacketEnvelope.toHex(chunk),
                        uuid = char.uuid.toString()
                    )
                }

                updateDiag { copy(packetsSent = packetsSent + 1) }
                Result.success(Unit)
            }
        }

    private suspend fun writeSingleChunk(
        gatt: BluetoothGatt,
        char: BluetoothGattCharacteristic,
        chunk: ByteArray,
        writeType: WriteType
    ): Result<Unit> {
        val gattWriteType = when (writeType) {
            WriteType.DEFAULT -> {
                if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                    return failWrite("Characteristic is not writable (WRITE)")
                }
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
            WriteType.NO_RESPONSE -> {
                if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                } else {
                    return failWrite("Characteristic is not writable")
                }
            }
        }

        char.writeType = gattWriteType

        val ack = CompletableDeferred<Int>()
        if (gattWriteType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
            writeAck.set(ack)
        }

        val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, chunk, gattWriteType) == BluetoothStatusCodes_SUCCESS()
        } else {
            @Suppress("DEPRECATION")
            char.value = chunk
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }

        if (!accepted) {
            writeAck.set(null)
            return failWrite("GATT writeCharacteristic returned false / rejected")
        }

        return if (gattWriteType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
            try {
                val status = withTimeout(5_000) { ack.await() }
                updateDiag { copy(lastGattStatus = status) }
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failWrite("GATT status $status")
                } else {
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                failWrite("GATT write timeout")
            } finally {
                writeAck.set(null)
            }
        } else {
            // Pace NO_RESPONSE slightly (reference relies on caller throttle for init)
            delay(15)
            Result.success(Unit)
        }
    }

    private fun BluetoothStatusCodes_SUCCESS(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            0
        }

    override suspend fun disconnect() {
        intentionalDisconnect = true
        sessionInitialized = false
        servicesDiscovered = false
        notificationSetupPending = false
        protocol.resetSession()
        try {
            notifyCharacteristic?.let { nc ->
                bluetoothGatt?.setCharacteristicNotification(nc, false)
            }
            bluetoothGatt?.disconnect()
            // close only after real disconnect path; give stack a beat then close
            delay(200)
            bluetoothGatt?.close()
        } catch (e: Exception) {
            log(LogLevel.ERROR, "BLE", "Error during disconnect: ${e.message}")
        } finally {
            bluetoothGatt = null
            writeCharacteristic = null
            notifyCharacteristic = null
            _connectionState.value = ConnectionState.Disconnected
            updateDiag {
                copy(
                    connected = false,
                    servicesDiscovered = false,
                    sessionInitialized = false
                )
            }
            log(LogLevel.INFO, "BLE", "Disconnected")
        }
    }

    suspend fun discoverServicesNow(): Result<Unit> = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext Result.failure(IllegalStateException("GATT not connected"))
        discoverDeferred = CompletableDeferred()
        if (!gatt.discoverServices()) {
            return@withContext Result.failure(IllegalStateException("discoverServices returned false"))
        }
        try {
            withTimeout(15_000) { discoverDeferred!!.await() }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Service discovery timeout", e))
        }
    }

    suspend fun requestMtuNow(mtu: Int = 512): Result<Int> = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext Result.failure(IllegalStateException("GATT not connected"))
        mtuDeferred = CompletableDeferred()
        if (!gatt.requestMtu(mtu)) {
            return@withContext Result.failure(IllegalStateException("requestMtu returned false"))
        }
        try {
            val negotiated = withTimeout(8_000) { mtuDeferred!!.await() }
            Result.success(negotiated)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("MTU request timeout", e))
        }
    }

    fun clearHexLog() {
        synchronized(debugHexLog) { debugHexLog.clear() }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (gatt == null) return
            if (bluetoothGatt !== gatt) {
                log(
                    LogLevel.DEBUG,
                    "BLE",
                    "Ignoring callback from an old GATT connection (status=$status, state=$newState)"
                )
                runCatching { gatt.close() }
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                val err = "GATT error status: $status on state: $newState"
                log(LogLevel.ERROR, "BLE", err)
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    log(
                        LogLevel.WARN,
                        "BLE",
                        "Connection dropped (status=$status). Possible other client (e.g. Official Divoom app) — unconfirmed."
                    )
                }
                servicesDiscovered = false
                sessionInitialized = false
                notificationSetupPending = false
                writeCharacteristic = null
                notifyCharacteristic = null
                _connectionState.value = ConnectionState.Error(err)
                updateDiag {
                    copy(
                        connected = false,
                        servicesDiscovered = false,
                        sessionInitialized = false,
                        notificationsEnabled = false,
                        lastError = err,
                        lastGattStatus = status
                    )
                }
                writeAck.getAndSet(null)?.complete(status)
                connectDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred = null
                runCatching { gatt.close() }
                if (bluetoothGatt === gatt) bluetoothGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log(LogLevel.INFO, "BLE", "GATT Connected. Requesting MTU 512...")
                    updateDiag { copy(connected = true) }
                    mtuDeferred = CompletableDeferred()
                    val mtuRequested = gatt.requestMtu(512)
                    if (!mtuRequested) {
                        currentMtu = 23
                        mtuDeferred?.complete(currentMtu)
                        mtuDeferred = null
                        updateDiag { copy(mtu = currentMtu) }
                        log(
                            LogLevel.WARN,
                            "BLE",
                            "MTU request was rejected; discovering services with MTU 23"
                        )
                        gatt.discoverServices()
                    } else {
                        scope.launch {
                            delay(2_500)
                            if (bluetoothGatt === gatt && !servicesDiscovered) {
                                log(
                                    LogLevel.WARN,
                                    "BLE",
                                    "MTU callback delayed; continuing service discovery"
                                )
                                gatt.discoverServices()
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log(LogLevel.WARN, "BLE", "GATT Disconnected")
                    if (!intentionalDisconnect &&
                        (_connectionState.value is ConnectionState.Connected ||
                            _connectionState.value is ConnectionState.Connecting)
                    ) {
                        log(
                            LogLevel.WARN,
                            "BLE",
                            "Link dropped. Possible other BLE client competing — unconfirmed without evidence."
                        )
                    }
                    servicesDiscovered = false
                    sessionInitialized = false
                    notificationSetupPending = false
                    writeCharacteristic = null
                    notifyCharacteristic = null
                    _connectionState.value = ConnectionState.Disconnected
                    updateDiag {
                        copy(
                            connected = false,
                            servicesDiscovered = false,
                            sessionInitialized = false,
                            notificationsEnabled = false
                        )
                    }
                    // Close after real disconnect
                    try {
                        gatt.close()
                    } catch (_: Exception) {
                    }
                    if (bluetoothGatt === gatt) bluetoothGatt = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (gatt == null || bluetoothGatt !== gatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentMtu = mtu
                log(LogLevel.INFO, "BLE", "MTU Negotiated: $mtu")
            } else {
                log(LogLevel.WARN, "BLE", "MTU Negotiation failed status=$status, using $currentMtu")
            }
            updateDiag { copy(mtu = currentMtu) }
            mtuDeferred?.complete(currentMtu)
            mtuDeferred = null

            // Continue connect pipeline: discover services
            if (!servicesDiscovered) {
                log(LogLevel.INFO, "BLE", "Discovering GATT Services...")
                discoverDeferred = discoverDeferred ?: CompletableDeferred()
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (gatt == null || bluetoothGatt !== gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val err = "Service Discovery Failed with status: $status"
                log(LogLevel.ERROR, "BLE", err)
                discoverDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred = null
                return
            }

            servicesDiscovered = true
            val report = buildGattReport(gatt)
            gattChars.clear()
            gattChars.addAll(parseGatt(gatt))

            writeCharacteristic = resolveWriteCharacteristic(gatt)
            notifyCharacteristic = gatt.getService(ProtocolUuids.SERVICE)
                ?.getCharacteristic(ProtocolUuids.NOTIFY_CHAR)

            val w = writeCharacteristic
            if (w == null) {
                val err = "Write characteristic not found (expected ${ProtocolUuids.WRITE_CHAR_STRING})"
                log(LogLevel.ERROR, "BLE", err)
                updateDiag {
                    copy(
                        servicesDiscovered = true,
                        gattReport = report,
                        lastError = err
                    )
                }
                discoverDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred = null
                return
            }

            if (!isWritable(w)) {
                val err = "Characteristic is not writable"
                log(LogLevel.ERROR, "BLE", err)
                discoverDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred?.complete(Result.failure(IllegalStateException(err)))
                connectDeferred = null
                return
            }

            updateDiag {
                copy(
                    servicesDiscovered = true,
                    serviceUuid = ProtocolUuids.SERVICE_STRING,
                    characteristicUuid = w.uuid.toString(),
                    mtu = currentMtu,
                    gattReport = report,
                    lastError = null
                )
            }

            // Android only allows one GATT operation at a time. Wait until the CCCD
            // subscription has completed before allowing session writes to begin.
            val nc = notifyCharacteristic
            val cccd = nc?.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            if (nc != null && cccd != null && gatt.setCharacteristicNotification(nc, true)) {
                notificationSetupPending = true
                val descriptorWriteStarted =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(
                            cccd,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(cccd)
                    }
                if (descriptorWriteStarted) {
                    scope.launch {
                        delay(1_500)
                        if (notificationSetupPending && bluetoothGatt === gatt) {
                            notificationSetupPending = false
                            log(
                                LogLevel.WARN,
                                "BLE",
                                "Notification setup timed out; continuing without notification confirmation"
                            )
                            completeServiceDiscovery(gatt, notificationsEnabled = false)
                        }
                    }
                    return
                }
                notificationSetupPending = false
            }

            completeServiceDiscovery(gatt, notificationsEnabled = false)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (!notificationSetupPending || gatt == null || bluetoothGatt !== gatt) return
            notificationSetupPending = false
            val enabled = status == BluetoothGatt.GATT_SUCCESS
            if (!enabled) {
                log(LogLevel.WARN, "BLE", "Notification descriptor write failed status=$status")
            }
            completeServiceDiscovery(gatt, notificationsEnabled = enabled)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (gatt == null || bluetoothGatt !== gatt) return
            updateDiag { copy(lastGattStatus = status) }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log(
                    LogLevel.ERROR,
                    "BLE",
                    "onCharacteristicWrite failed status=$status",
                    uuid = characteristic?.uuid?.toString()
                )
            }
            writeAck.getAndSet(null)?.complete(status)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (gatt == null || bluetoothGatt !== gatt) return
            handleNotification(characteristic, characteristic?.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (bluetoothGatt !== gatt) return
            handleNotification(characteristic, value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (gatt == null || bluetoothGatt !== gatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateDiag { copy(rssi = rssi) }
            }
        }
    }

    private fun handleNotification(
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray
    ) {
        val hex = PacketEnvelope.toHex(value)
        appendHexLog("RX $hex")
        updateDiag { copy(lastNotificationHex = hex.take(512)) }
        log(
            LogLevel.RX,
            "BLE",
            "Notification (${value.size} B)",
            hexData = hex,
            uuid = characteristic?.uuid?.toString()
        )
    }

    private fun completeServiceDiscovery(
        gatt: BluetoothGatt,
        notificationsEnabled: Boolean
    ) {
        if (bluetoothGatt !== gatt) return
        val w = writeCharacteristic ?: return
        val name = gatt.device?.name ?: "Divoom"
        val addr = gatt.device?.address ?: targetAddress.orEmpty()
        _connectionState.value = ConnectionState.Connected(
            deviceName = name,
            deviceAddress = addr,
            connectionType = ConnectionType.BLE,
            mtu = currentMtu
        )
        updateDiag {
            copy(
                connected = true,
                servicesDiscovered = true,
                serviceUuid = ProtocolUuids.SERVICE_STRING,
                characteristicUuid = w.uuid.toString(),
                mtu = currentMtu,
                deviceName = name,
                deviceAddress = addr,
                notificationsEnabled = notificationsEnabled,
                lastError = null
            )
        }
        log(
            LogLevel.INFO,
            "BLE",
            "Services ready. Write char=${w.uuid}; notifications=$notificationsEnabled"
        )
        discoverDeferred?.complete(Result.success(Unit))
        discoverDeferred = null
        connectDeferred?.complete(Result.success(Unit))
        connectDeferred = null
    }

    private fun resolveWriteCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        if (gatt == null) return null
        advancedModeManualChar?.let { (svc, ch) ->
            gatt.getService(svc)?.getCharacteristic(ch)?.let { return it }
        }
        gatt.getService(ProtocolUuids.SERVICE)?.getCharacteristic(ProtocolUuids.WRITE_CHAR)?.let {
            return it
        }
        // Fallback: find 8841 anywhere
        for (svc in gatt.services.orEmpty()) {
            for (ch in svc.characteristics) {
                if (ch.uuid == ProtocolUuids.WRITE_CHAR) return ch
            }
        }
        return null
    }

    private fun isWritable(char: BluetoothGattCharacteristic): Boolean {
        val p = char.properties
        return (p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
            (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }

    private fun parseGatt(gatt: BluetoothGatt): List<GattCharacteristicInfo> {
        val list = mutableListOf<GattCharacteristicInfo>()
        for (service in gatt.services) {
            for (char in service.characteristics) {
                list.add(
                    GattCharacteristicInfo(
                        serviceUuid = service.uuid.toString(),
                        characteristicUuid = char.uuid.toString(),
                        properties = parseProperties(char.properties),
                        descriptors = char.descriptors.map { it.uuid.toString() }
                    )
                )
            }
        }
        return list
    }

    private fun buildGattReport(gatt: BluetoothGatt): String {
        val sb = StringBuilder()
        for (service in gatt.services) {
            sb.appendLine("Service: ${service.uuid}")
            for (char in service.characteristics) {
                val props = parseProperties(char.properties).joinToString("|")
                sb.appendLine("  Char: ${char.uuid} [$props]")
                for (d in char.descriptors) {
                    sb.appendLine("    Desc: ${d.uuid}")
                }
            }
        }
        return sb.toString()
    }

    private fun parseProperties(props: Int): List<String> {
        val list = mutableListOf<String>()
        if ((props and BluetoothGattCharacteristic.PROPERTY_READ) != 0) list.add("READ")
        if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) list.add("WRITE")
        if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) list.add("WRITE_NO_RESPONSE")
        if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) list.add("NOTIFY")
        if ((props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) list.add("INDICATE")
        return list
    }

    private fun failWrite(msg: String): Result<Unit> {
        log(LogLevel.ERROR, "BLE", msg)
        updateDiag { copy(lastError = msg, packetsFailed = packetsFailed + 1) }
        return Result.failure(IllegalStateException(msg))
    }

    private fun appendHexLog(line: String) {
        synchronized(debugHexLog) {
            debugHexLog.addLast(line)
            while (debugHexLog.size > maxHexLogLines) debugHexLog.removeFirst()
        }
    }

    private fun updateDiag(block: TransportDiagnostics.() -> TransportDiagnostics) {
        _diagnostics.update { it.block() }
    }

    private fun log(
        level: LogLevel,
        tag: String,
        msg: String,
        hexData: String? = null,
        uuid: String? = null
    ) {
        scope.launch {
            logFlow.emit(LogEntry(tag = tag, message = msg, hexData = hexData, uuid = uuid, level = level))
        }
    }
}
