package com.divoomspeed.backpack.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.divoomspeed.backpack.logging.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class BleGattTransport(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter(),
    private val writeCharacteristicUuidStr: String = "0000ffe1-0000-1000-8000-00805f9b34fb",
    private val writeWithoutResponse: Boolean = true
) : DivoomTransport {

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null
    private var negotiatedMtu: Int = 23
    private val sendMutex = Mutex()

    private var writeCompletionDeferred: CompletableDeferred<Unit>? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    DebugLogger.i("Bluetooth", "GATT Connected, requesting MTU 512...")
                    _connectionState.value = BluetoothConnectionState.Connecting
                    gatt.requestMtu(512)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    DebugLogger.i("Bluetooth", "GATT Disconnected")
                    _connectionState.value = BluetoothConnectionState.Idle
                    closeGatt()
                }
            } else {
                val err = "GATT Error status: $status"
                DebugLogger.e("Bluetooth", err)
                _connectionState.value = BluetoothConnectionState.Error(err)
                closeGatt()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else 23
            DebugLogger.i("Bluetooth", "Negotiated BLE MTU: $negotiatedMtu")
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                DebugLogger.i("Bluetooth", "BLE Services discovered (${gatt.services.size} services)")
                findTargetCharacteristic(gatt)
                if (targetCharacteristic != null) {
                    _connectionState.value = BluetoothConnectionState.Connected
                    DebugLogger.i("Bluetooth", "BLE Write Characteristic Ready: ${targetCharacteristic?.uuid}")
                } else {
                    val err = "Target Write Characteristic not found ($writeCharacteristicUuidStr)"
                    DebugLogger.e("Bluetooth", err)
                    _connectionState.value = BluetoothConnectionState.Error(err)
                }
            } else {
                _connectionState.value = BluetoothConnectionState.Error("Service discovery failed: $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCompletionDeferred?.complete(Unit)
            } else {
                writeCompletionDeferred?.completeExceptionally(IllegalStateException("Write failed status $status"))
            }
        }
    }

    private fun findTargetCharacteristic(gatt: BluetoothGatt) {
        val targetUuid = UUID.fromString(writeCharacteristicUuidStr)
        for (service in gatt.services) {
            for (char in service.characteristics) {
                if (char.uuid == targetUuid) {
                    targetCharacteristic = char
                    return
                }
            }
        }
        // Fallback search for any characteristic with WRITE property
        for (service in gatt.services) {
            for (char in service.characteristics) {
                val props = char.properties
                if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) ||
                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)) {
                    targetCharacteristic = char
                    DebugLogger.w("Bluetooth", "Using fallback BLE write characteristic: ${char.uuid}")
                    return
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth disabled"))
        }

        _connectionState.value = BluetoothConnectionState.Connecting
        DebugLogger.i("Bluetooth", "Connecting BLE GATT to $deviceAddress")

        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BluetoothConnectionState.Error("BLE Connect failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        _connectionState.value = BluetoothConnectionState.Disconnecting
        closeGatt()
        _connectionState.value = BluetoothConnectionState.Idle
        DebugLogger.i("Bluetooth", "BLE GATT disconnected")
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (ignored: Exception) {
        } finally {
            bluetoothGatt = null
            targetCharacteristic = null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val gatt = bluetoothGatt
            val char = targetCharacteristic
            if (gatt == null || char == null) {
                val err = "BLE GATT is not connected or characteristic missing"
                DebugLogger.e("Bluetooth", err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            val chunkSize = (negotiatedMtu - 3).coerceAtLeast(20)
            var offset = 0

            try {
                while (offset < data.size) {
                    val end = (offset + chunkSize).coerceAtMost(data.size)
                    val chunk = data.copyOfRange(offset, end)

                    writeCompletionDeferred = CompletableDeferred()

                    val writeType = if (writeWithoutResponse) {
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    } else {
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    }

                    char.writeType = writeType
                    char.value = chunk

                    val success = gatt.writeCharacteristic(char)
                    if (!success) {
                        return@withContext Result.failure(IllegalStateException("writeCharacteristic returned false"))
                    }

                    if (writeWithoutResponse) {
                        // Small delay for write without response to prevent controller buffer overflow
                        kotlinx.coroutines.delay(15L)
                    } else {
                        // Wait for GATT callback
                        writeCompletionDeferred?.await()
                    }

                    offset = end
                }
                val hexStr = data.joinToString(" ") { "%02X".format(it) }
                DebugLogger.d("Bluetooth", "Sent ${data.size} bytes over BLE (Chunk size: $chunkSize)", hexStr)
                Result.success(Unit)
            } catch (e: Exception) {
                val err = "BLE Send error: ${e.localizedMessage}"
                DebugLogger.e("Bluetooth", err)
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
            val gatt = bluetoothGatt
            val bleCaps = mutableListOf<BleGattCapability>()

            if (gatt != null) {
                for (service in gatt.services) {
                    for (char in service.characteristics) {
                        val props = char.properties
                        bleCaps.add(
                            BleGattCapability(
                                serviceUuid = service.uuid.toString(),
                                characteristicUuid = char.uuid.toString(),
                                canWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0,
                                canWriteNoResponse = (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0,
                                canNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                            )
                        )
                    }
                }
            }

            val caps = DeviceCapabilities(
                deviceName = device.name ?: "Unknown BLE Divoom Device",
                deviceAddress = device.address,
                deviceType = DivoomDeviceType.BLE_GATT,
                isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                bluetoothClass = device.bluetoothClass?.deviceClass?.toString() ?: "Unknown",
                sdpUuids = device.uuids?.map { it.uuid.toString() } ?: emptyList(),
                bleCapabilities = bleCaps,
                maxMtu = negotiatedMtu,
                protocolVerified = bleCaps.isNotEmpty(),
                protocolName = "Divoom BLE GATT"
            )
            Result.success(caps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
