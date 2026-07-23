package com.divoomspeed.backpack.bluetooth

sealed interface BluetoothConnectionState {
    data object Idle : BluetoothConnectionState
    data object Scanning : BluetoothConnectionState
    data object Connecting : BluetoothConnectionState
    data object Connected : BluetoothConnectionState
    data object Disconnecting : BluetoothConnectionState
    data class Reconnecting(val attempt: Int) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}
