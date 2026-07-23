package com.divoomspeed.backpack.protocol

import com.divoomspeed.backpack.bluetooth.DeviceCapabilities
import com.divoomspeed.backpack.renderer.PixelFrame

interface DivoomProtocolEncoder {
    val protocolName: String

    fun supports(device: DeviceCapabilities): Boolean

    fun encodeFrame(frame: PixelFrame): Result<List<ByteArray>>

    fun encodeBrightness(brightness: Int): Result<List<ByteArray>>

    fun encodeClearScreen(): Result<List<ByteArray>>

    fun encodeSelectChannel(channel: Byte = 0x03.toByte()): Result<List<ByteArray>>

    companion object {
        fun calculateChecksum(command: Byte, payload: ByteArray): Int {
            var sum = (command.toInt() and 0xFF)
            for (b in payload) {
                sum += (b.toInt() and 0xFF)
            }
            return sum and 0xFFFF
        }

        fun applyByteEscaping(data: ByteArray): ByteArray {
            val result = ArrayList<Byte>()
            for (b in data) {
                when (b) {
                    0x01.toByte() -> {
                        result.add(0x03.toByte())
                        result.add(0x04.toByte())
                    }
                    0x02.toByte() -> {
                        result.add(0x03.toByte())
                        result.add(0x05.toByte())
                    }
                    0x03.toByte() -> {
                        result.add(0x03.toByte())
                        result.add(0x06.toByte())
                    }
                    else -> result.add(b)
                }
            }
            return result.toByteArray()
        }

        fun buildPacket(command: Byte, payload: ByteArray): ByteArray {
            val length = payload.size + 3 // 1 cmd + 2 crc
            val checksum = calculateChecksum(command, payload)

            val rawBody = ByteArray(1 + 2 + 1 + payload.size + 2)
            rawBody[0] = 0x01.toByte() // Start byte
            rawBody[1] = (length and 0xFF).toByte()
            rawBody[2] = ((length shr 8) and 0xFF).toByte()
            rawBody[3] = command
            System.arraycopy(payload, 0, rawBody, 4, payload.size)
            val crcIdx = 4 + payload.size
            rawBody[crcIdx] = (checksum and 0xFF).toByte()
            rawBody[crcIdx + 1] = ((checksum shr 8) and 0xFF).toByte()

            // Escape bytes from index 1 to crcIdx + 1
            val bodyToEscape = rawBody.copyOfRange(1, rawBody.size)
            val escapedBody = applyByteEscaping(bodyToEscape)

            val finalPacket = ByteArray(1 + escapedBody.size + 1)
            finalPacket[0] = 0x01.toByte()
            System.arraycopy(escapedBody, 0, finalPacket, 1, escapedBody.size)
            finalPacket[finalPacket.size - 1] = 0x02.toByte() // End byte

            return finalPacket
        }
    }
}
