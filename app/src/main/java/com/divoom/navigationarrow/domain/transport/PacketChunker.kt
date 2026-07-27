package com.divoom.navigationarrow.domain.transport

import com.divoom.navigationarrow.domain.protocol.ProtocolUuids

/**
 * Splits a full envelope packet into app-layer BLE write chunks (138 bytes default).
 * Matches orionparrott/divoom-protocol DivoomClient.CHUNK_SIZE.
 */
object PacketChunker {
    fun chunk(packet: ByteArray, chunkSize: Int = ProtocolUuids.APP_CHUNK_SIZE): List<ByteArray> {
        require(chunkSize > 0) { "chunkSize must be positive" }
        if (packet.isEmpty()) return emptyList()
        val result = ArrayList<ByteArray>()
        var offset = 0
        while (offset < packet.size) {
            val end = minOf(offset + chunkSize, packet.size)
            result.add(packet.copyOfRange(offset, end))
            offset = end
        }
        return result
    }
}

enum class WriteType {
    /** ATT write with response — wait for onCharacteristicWrite */
    DEFAULT,
    /** ATT write without response */
    NO_RESPONSE
}
