package com.divoomspeed.backpack.bluetooth

import kotlinx.coroutines.flow.StateFlow

interface DivoomTransport {
    val connectionState: StateFlow<BluetoothConnectionState>

    suspend fun connect(deviceAddress: String): Result<Unit>

    suspend fun disconnect()

    suspend fun send(data: ByteArray): Result<Unit>

    suspend fun discoverDeviceCapabilities(deviceAddress: String): Result<DeviceCapabilities>
}
