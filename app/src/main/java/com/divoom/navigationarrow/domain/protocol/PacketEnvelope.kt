package com.divoom.navigationarrow.domain.protocol

/**
 * Port of divoom_protocol.packet.build_packet
 * FE EF AA 55 envelope — commit f9b661f8e06a6d40be91c800fed656c7402bd2ac
 */
object PacketEnvelope {

    fun buildPacket(payload: ByteArray, session: DivoomSession): ByteArray {
        val seq = session.nextSeq()
        val seqHi = (seq shr 8) and 0xFF
        val seqLo = seq and 0xFF

        // content_len = seq(2) + zeros(3) + payload + crc(2)
        val contentLen = 2 + 3 + payload.size + 2
        val lenLo = contentLen and 0xFF
        val lenHi = (contentLen shr 8) and 0xFF

        val contentData = ByteArray(5 + payload.size)
        contentData[0] = seqHi.toByte()
        contentData[1] = seqLo.toByte()
        contentData[2] = 0x00
        contentData[3] = 0x00
        contentData[4] = 0x00
        System.arraycopy(payload, 0, contentData, 5, payload.size)

        var crcSum = lenLo + lenHi
        for (b in contentData) {
            crcSum += b.toInt() and 0xFF
        }
        val crc = crcSum and 0xFFFF
        val crcLo = crc and 0xFF
        val crcHi = (crc shr 8) and 0xFF

        val out = ByteArray(6 + contentData.size + 2)
        out[0] = 0xFE.toByte()
        out[1] = 0xEF.toByte()
        out[2] = 0xAA.toByte()
        out[3] = 0x55.toByte()
        out[4] = lenLo.toByte()
        out[5] = lenHi.toByte()
        System.arraycopy(contentData, 0, out, 6, contentData.size)
        out[out.size - 2] = crcLo.toByte()
        out[out.size - 1] = crcHi.toByte()
        return out
    }

    fun toHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
