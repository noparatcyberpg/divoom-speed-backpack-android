package com.divoom.navigationarrow.domain.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(
        val deviceName: String,
        val deviceAddress: String,
        val connectionType: ConnectionType,
        val mtu: Int = 23
    ) : ConnectionState()
    data class Error(val message: String, val throwable: Throwable? = null) : ConnectionState()
}

enum class ConnectionType {
    BLE,
    CLASSIC,
    UNKNOWN
}
