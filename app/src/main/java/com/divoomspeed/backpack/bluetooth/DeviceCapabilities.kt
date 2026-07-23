package com.divoomspeed.backpack.bluetooth

enum class DivoomDeviceType {
    CLASSIC_RFCOMM,
    BLE_GATT,
    DUAL_MODE,
    UNKNOWN
}

data class BleGattCapability(
    val serviceUuid: String,
    val characteristicUuid: String,
    val canWrite: Boolean,
    val canWriteNoResponse: Boolean,
    val canNotify: Boolean
)

data class DeviceCapabilities(
    val deviceName: String,
    val deviceAddress: String,
    val deviceType: DivoomDeviceType,
    val isBonded: Boolean,
    val bluetoothClass: String,
    val sdpUuids: List<String> = emptyList(),
    val rfcommChannels: List<Int> = listOf(1, 2),
    val bleCapabilities: List<BleGattCapability> = emptyList(),
    val maxMtu: Int = 23,
    val protocolVerified: Boolean = false,
    val protocolName: String = "Unknown Protocol"
)
