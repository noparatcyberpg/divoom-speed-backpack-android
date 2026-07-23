package com.divoomspeed.backpack

import com.divoomspeed.backpack.protocol.DivoomProtocolEncoder
import com.divoomspeed.backpack.protocol.LegacyDivoomProtocolEncoder
import com.divoomspeed.backpack.renderer.PixelFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivoomProtocolEncoderTest {

    @Test
    fun testChecksumCalculation() {
        val cmd: Byte = 0x44
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val checksum = DivoomProtocolEncoder.calculateChecksum(cmd, payload)
        // 0x44 (68) + 1 + 2 + 3 = 74 (0x004A)
        assertEquals(74, checksum)
    }

    @Test
    fun testByteEscaping() {
        val raw = byteArrayOf(0x01, 0x02, 0x03, 0x05)
        val escaped = DivoomProtocolEncoder.applyByteEscaping(raw)
        // 0x01 -> 0x03 0x04, 0x02 -> 0x03 0x05, 0x03 -> 0x03 0x06, 0x05 -> 0x05
        val expected = byteArrayOf(0x03, 0x04, 0x03, 0x05, 0x03, 0x06, 0x05)
        assertTrue(expected.contentEquals(escaped))
    }

    @Test
    fun testBuildPacketFraming() {
        val cmd: Byte = 0x45
        val payload = byteArrayOf(0x00)
        val packet = DivoomProtocolEncoder.buildPacket(cmd, payload)

        // Check start byte (0x01) and end byte (0x02)
        assertEquals(0x01.toByte(), packet[0])
        assertEquals(0x02.toByte(), packet[packet.size - 1])
    }

    @Test
    fun testLegacyEncoderFrameEncoding() {
        val encoder = LegacyDivoomProtocolEncoder()
        val dummyPixels = IntArray(256) { 0 }
        val frame = PixelFrame(16, 16, dummyPixels)

        val result = encoder.encodeFrame(frame)
        assertTrue(result.isSuccess)
        val packets = result.getOrNull()
        assertTrue(!packets.isNullOrEmpty())
    }
}
