package com.divoomspeed.backpack.bluetooth

import com.divoomspeed.backpack.logging.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDivoomTransport : DivoomTransport {

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    override val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    var lastSentData: ByteArray? = null
        private set

    override suspend fun connect(deviceAddress: String): Result<Unit> {
        _connectionState.value = BluetoothConnectionState.Connecting
        kotlinx.coroutines.delay(200L)
        _connectionState.value = BluetoothConnectionState.Connected
        DebugLogger.i("Bluetooth", "FakeDivoomTransport connected to $deviceAddress")
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = BluetoothConnectionState.Disconnecting
        kotlinx.coroutines.delay(100L)
        _connectionState.value = BluetoothConnectionState.Idle
        DebugLogger.i("Bluetooth", "FakeDivoomTransport disconnected")
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        if (_connectionState.value != BluetoothConnectionState.Connected) {
            return Result.failure(IllegalStateException("Fake transport not connected"))
        }
        lastSentData = data
        val hexStr = data.joinToString(" ") { "%02X".format(it) }
        DebugLogger.d("Bluetooth", "FakeDivoomTransport received ${data.size} bytes", hexStr)
        return Result.success(Unit)
    }

    override suspend fun discoverDeviceCapabilities(deviceAddress: String): Result<DeviceCapabilities> {
        val caps = DeviceCapabilities(
            deviceName = "Fake Divoom Pixoo Backpack",
            deviceAddress = deviceAddress,
            deviceType = DivoomDeviceType.DUAL_MODE,
            isBonded = true,
            bluetoothClass = "Audio/Video Handsfree",
            sdpUuids = listOf("00001101-0000-1000-8000-00805F9B34FB"),
            protocolVerified = true,
            protocolName = "Fake Test Protocol"
        )
        return Result.success(caps)
    }
}
