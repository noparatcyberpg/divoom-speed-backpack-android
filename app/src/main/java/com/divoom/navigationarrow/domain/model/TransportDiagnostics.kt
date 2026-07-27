package com.divoom.navigationarrow.domain.model

data class TransportDiagnostics(
    val deviceAddress: String? = null,
    val connected: Boolean = false,
    val servicesDiscovered: Boolean = false,
    val sessionInitialized: Boolean = false,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null,
    val mtu: Int? = null,
    val packetsQueued: Int = 0,
    val packetsSent: Int = 0,
    val packetsFailed: Int = 0,
    val lastPacketHex: String? = null,
    val lastNotificationHex: String? = null,
    val lastError: String? = null,
    val lastGattStatus: Int? = null,
    val writeType: String? = null,
    val notificationsEnabled: Boolean = false,
    val deviceName: String? = null,
    val deviceType: String? = null,
    val bondState: String? = null,
    val rssi: Int? = null,
    val gattReport: String? = null
)

data class DisplayState(
    val pendingFrame: PixelFrame? = null,
    val lastSuccessfullySentFrame: PixelFrame? = null,
    val lastSentAt: Long? = null,
    val lastSendResult: String? = null,
    val lastError: String? = null,
    val lastPacketCount: Int = 0,
    val lastBytesSent: Int = 0
)

data class GattCharacteristicInfo(
    val serviceUuid: String,
    val characteristicUuid: String,
    val properties: List<String>,
    val descriptors: List<String>
)
