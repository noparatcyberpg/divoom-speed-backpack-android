package com.divoomspeed.backpack.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.divoomspeed.backpack.logging.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ClassicRfcommTransport(
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter(),
    private val targetUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"),
    private val customChannel: Int = 1,
    private val useInsecure: Boolean = false
) : DivoomTransport {

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _incomingBytes = MutableStateFlow<ByteArray>(byteArrayOf())
    val incomingBytes: StateFlow<ByteArray> = _incomingBytes.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private val sendMutex = Mutex()

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val err = "Bluetooth adapter disabled or unavailable"
            _connectionState.value = BluetoothConnectionState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        _connectionState.value = BluetoothConnectionState.Connecting
        DebugLogger.i("Bluetooth", "Connecting RFCOMM to $deviceAddress (UUID: $targetUuid, Channel: $customChannel, Insecure: $useInsecure)")

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            bluetoothAdapter.cancelDiscovery()

            // Try standard UUID connection first, fallback to reflection channel socket
            var connectedSocket: BluetoothSocket? = null

            try {
                connectedSocket = if (useInsecure) {
                    device.createInsecureRfcommSocketToServiceRecord(targetUuid)
                } else {
                    device.createRfcommSocketToServiceRecord(targetUuid)
                }
                connectedSocket.connect()
                DebugLogger.i("Bluetooth", "RFCOMM connected via Service Record UUID: $targetUuid")
            } catch (e: Exception) {
                DebugLogger.w("Bluetooth", "Service record connection failed, trying channel $customChannel reflection socket...")
                val m = device.javaClass.getMethod(
                    if (useInsecure) "createInsecureRfcommSocket" else "createRfcommSocket",
                    Int::class.javaPrimitiveType
                )
                connectedSocket = m.invoke(device, customChannel) as BluetoothSocket
                connectedSocket.connect()
                DebugLogger.i("Bluetooth", "RFCOMM connected via reflection channel socket $customChannel")
            }

            socket = connectedSocket
            outputStream = connectedSocket.outputStream
            inputStream = connectedSocket.inputStream

            readJob?.cancel()
            readJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(1024)
                try {
                    while (isActive && socket?.isConnected == true) {
                        val stream = inputStream ?: break
                        val count = stream.read(buffer)
                        if (count > 0) {
                            val received = buffer.copyOfRange(0, count)
                            _incomingBytes.value = received
                            val hexStr = received.joinToString(" ") { "%02X".format(it) }
                            DebugLogger.i("Bluetooth", "Received $count bytes from Divoom: $hexStr")
                        }
                    }
                } catch (ignored: Exception) {
                }
            }

            _connectionState.value = BluetoothConnectionState.Connected
            Result.success(Unit)
        } catch (e: Exception) {
            val err = "RFCOMM Connection failed: ${e.localizedMessage}"
            DebugLogger.e("Bluetooth", err)
            disconnectInternal()
            _connectionState.value = BluetoothConnectionState.Error(err)
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        _connectionState.value = BluetoothConnectionState.Disconnecting
        disconnectInternal()
        _connectionState.value = BluetoothConnectionState.Idle
        DebugLogger.i("Bluetooth", "RFCOMM Transport disconnected")
    }

    private fun disconnectInternal() {
        try {
            readJob?.cancel()
            readJob = null
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (ignored: Exception) {
        } finally {
            outputStream = null
            inputStream = null
            socket = null
        }
    }

    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val stream = outputStream
            if (socket == null || !socket!!.isConnected || stream == null) {
                val err = "RFCOMM Socket is not connected"
                DebugLogger.e("Bluetooth", err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            try {
                val chunkSize = 256
                var offset = 0
                while (offset < data.size) {
                    val len = Math.min(chunkSize, data.size - offset)
                    stream.write(data, offset, len)
                    stream.flush()
                    offset += len
                    if (offset < data.size) {
                        Thread.sleep(15)
                    }
                }
                val hexStr = data.joinToString(" ") { "%02X".format(it) }
                DebugLogger.d("Bluetooth", "Sent ${data.size} bytes via RFCOMM in chunks", hexStr)
                Result.success(Unit)
            } catch (e: Exception) {
                val err = "RFCOMM Send error: ${e.localizedMessage}"
                DebugLogger.e("Bluetooth", err)
                _connectionState.value = BluetoothConnectionState.Error(err)
                Result.failure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun discoverDeviceCapabilities(deviceAddress: String): Result<DeviceCapabilities> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            return@withContext Result.failure(IllegalStateException("Bluetooth unavailable"))
        }

        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val sdpList = device.uuids?.map { it.uuid.toString() } ?: emptyList()
            val caps = DeviceCapabilities(
                deviceName = device.name ?: "Unknown Divoom Device",
                deviceAddress = device.address,
                deviceType = DivoomDeviceType.CLASSIC_RFCOMM,
                isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                bluetoothClass = device.bluetoothClass?.deviceClass?.toString() ?: "Unknown",
                sdpUuids = sdpList,
                rfcommChannels = listOf(1, 2, customChannel).distinct(),
                protocolVerified = true,
                protocolName = "Divoom RFCOMM SPP"
            )
            Result.success(caps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
