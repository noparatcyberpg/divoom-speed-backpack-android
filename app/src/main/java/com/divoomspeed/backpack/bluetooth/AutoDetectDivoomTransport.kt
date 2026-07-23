package com.divoomspeed.backpack.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.divoomspeed.backpack.logging.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoDetectDivoomTransport(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
) : DivoomTransport {

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private var activeTransport: DivoomTransport? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String): Result<Unit> {
        if (bluetoothAdapter == null) {
            val err = "Bluetooth adapter unavailable"
            _connectionState.value = BluetoothConnectionState.Error(err)
            return Result.failure(IllegalStateException(err))
        }

        _connectionState.value = BluetoothConnectionState.Connecting
        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val deviceType = device.type

        DebugLogger.i("Bluetooth", "AutoDetecting transport for $deviceAddress (BluetoothDevice Type: $deviceType)")

        val transport = when (deviceType) {
            BluetoothDevice.DEVICE_TYPE_LE -> {
                DebugLogger.i("Bluetooth", "Auto-selected BLE GATT Transport")
                BleGattTransport(context, bluetoothAdapter)
            }
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> {
                DebugLogger.i("Bluetooth", "Auto-selected Classic RFCOMM Transport")
                ClassicRfcommTransport(bluetoothAdapter)
            }
            BluetoothDevice.DEVICE_TYPE_DUAL -> {
                DebugLogger.i("Bluetooth", "Dual mode device detected (Backpack M). Trying RFCOMM first...")
                val classic = ClassicRfcommTransport(bluetoothAdapter)
                val classicRes = classic.connect(deviceAddress)
                if (classicRes.isSuccess) {
                    DebugLogger.i("Bluetooth", "RFCOMM connected successfully for Dual mode device")
                    classic
                } else {
                    DebugLogger.w("Bluetooth", "RFCOMM failed for Dual mode device. Trying BLE GATT Transport...")
                    val ble = BleGattTransport(context, bluetoothAdapter)
                    val bleRes = ble.connect(deviceAddress)
                    if (bleRes.isSuccess) {
                        DebugLogger.i("Bluetooth", "BLE GATT connected successfully for Dual mode device")
                        ble
                    } else {
                        classic
                    }
                }
            }
            else -> {
                DebugLogger.w("Bluetooth", "Unknown device type ($deviceType). Defaulting to Classic RFCOMM...")
                ClassicRfcommTransport(bluetoothAdapter)
            }
        }

        activeTransport = transport
        _connectionState.value = transport.connectionState.value
        return if (transport.connectionState.value is BluetoothConnectionState.Connected) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Connection failed for all transport modes"))
        }
    }

    override suspend fun disconnect() {
        _connectionState.value = BluetoothConnectionState.Disconnecting
        activeTransport?.disconnect()
        activeTransport = null
        _connectionState.value = BluetoothConnectionState.Idle
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        val transport = activeTransport ?: return Result.failure(IllegalStateException("No active transport connected"))
        return transport.send(data)
    }

    override suspend fun discoverDeviceCapabilities(deviceAddress: String): Result<DeviceCapabilities> {
        val transport = activeTransport ?: ClassicRfcommTransport(bluetoothAdapter)
        return transport.discoverDeviceCapabilities(deviceAddress)
    }
}
