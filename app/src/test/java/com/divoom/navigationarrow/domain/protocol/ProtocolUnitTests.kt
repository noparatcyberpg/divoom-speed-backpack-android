package com.divoom.navigationarrow.domain.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketEnvelopeTest {

    @Test
    fun firstPacketUsesSeq0001() {
        val s = DivoomSession()
        val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x18), s)
        assertEquals(0x00, pkt[6].toInt() and 0xFF)
        assertEquals(0x01, pkt[7].toInt() and 0xFF)
    }

    @Test
    fun authenticatedJumpsTo0101() {
        val s = DivoomSession()
        PacketEnvelope.buildPacket(byteArrayOf(0x99.toByte()), s)
        s.bumpToAuthenticated()
        val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x18), s)
        assertEquals(0x01, pkt[6].toInt() and 0xFF)
        assertEquals(0x01, pkt[7].toInt() and 0xFF)
    }

    @Test
    fun authenticatedSeqIncrementsNormally() {
        val s = DivoomSession()
        PacketEnvelope.buildPacket(byteArrayOf(0x99.toByte()), s)
        s.bumpToAuthenticated()
        val seqs = mutableListOf<Int>()
        repeat(5) {
            val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x46), s)
            seqs.add(((pkt[6].toInt() and 0xFF) shl 8) or (pkt[7].toInt() and 0xFF))
        }
        assertEquals(listOf(0x0101, 0x0102, 0x0103, 0x0104, 0x0105), seqs)
    }

    @Test
    fun magicBytes() {
        val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x18), DivoomSession())
        assertEquals(0xFE, pkt[0].toInt() and 0xFF)
        assertEquals(0xEF, pkt[1].toInt() and 0xFF)
        assertEquals(0xAA, pkt[2].toInt() and 0xFF)
        assertEquals(0x55, pkt[3].toInt() and 0xFF)
    }

    @Test
    fun contentLengthField() {
        val payload = byteArrayOf(0x74, 0x32)
        val pkt = PacketEnvelope.buildPacket(payload, DivoomSession())
        val expectedLen = 2 + 3 + payload.size + 2
        val actualLen = (pkt[4].toInt() and 0xFF) or ((pkt[5].toInt() and 0xFF) shl 8)
        assertEquals(expectedLen, actualLen)
    }

    @Test
    fun brightnessPacketMatchesPythonReference() {
        // Python: session.seq=0x0101; build_packet([0x74,0x32])
        val s = DivoomSession()
        s.seq = 0x0101
        val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x74, 0x32), s)
        val expected = hexToBytes("FE EF AA 55 09 00 01 01 00 00 00 74 32 B1 00")
        assertArrayEquals(expected, pkt)
    }

    @Test
    fun lightingRedMatchesPythonReference() {
        val s = DivoomSession()
        s.seq = 0x0101
        val payload = byteArrayOf(0x45, 0x01, 0xFF.toByte(), 0x00, 0x00, 100.toByte(), 0x00, 0x01)
        val pkt = PacketEnvelope.buildPacket(payload, s)
        val expected = hexToBytes("FE EF AA 55 0F 00 01 01 00 00 00 45 01 FF 00 00 64 00 01 BB 01")
        assertArrayEquals(expected, pkt)
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.trim().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()
}

class InitializationPacketTest {

    @Test
    fun setUtcHasJsonMarkerAndSeq0001ThenBumps() {
        val protocol = ModernBackpackBleProtocol(
            controllerUuid = "12345678-1234-1234-1234-123456789abc"
        )
        val packets = protocol.buildInitializationPackets()
        val setUtc = packets.first()
        assertEquals(0xFE, setUtc[0].toInt() and 0xFF)
        assertEquals(0x00, setUtc[6].toInt() and 0xFF)
        assertEquals(0x01, setUtc[7].toInt() and 0xFF)
        assertEquals(0x7B, setUtc[11].toInt() and 0xFF) // '{'
        // After SetUTC alone seq becomes 0x0101; after full init it advances further.
        assertTrue(protocol.sessionForTests().isAuthenticated())
        assertTrue(protocol.sessionForTests().seq > 0x0101)
        assertTrue(packets.size > 30)
        // Unlock pair near end
        val unlockBd = packets[packets.size - 2]
        assertEquals(0xBD, unlockBd[11].toInt() and 0xFF)
        assertEquals(0x2F, unlockBd[12].toInt() and 0xFF)
        val unlock31 = packets.last()
        assertEquals(0x31, unlock31[11].toInt() and 0xFF)
    }

    @Test
    fun deviceInfoIndexPointsToBa01() {
        val protocol = ModernBackpackBleProtocol()
        val packets = protocol.buildInitializationPackets()
        val idx = protocol.deviceInfoPacketIndex()
        assertEquals(0xBA, packets[idx][11].toInt() and 0xFF)
        assertEquals(0x01, packets[idx][12].toInt() and 0xFF)
    }
}

class StaticImageEncoderTest {

    @Test
    fun solidRedInnerMatchesPythonReference() {
        // Python encode_image(solid_frame(255,0,0))
        val frame = com.divoom.navigationarrow.domain.model.PixelFrame.solid(0xFF0000)
        val encoded = StaticImageEncoder.encodeImage(frame)
        val expected = hexToBytes(
            "44 00 0A 0A 04 AA 2A 00 F4 01 00 01 FF 00 00 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        )
        assertArrayEquals(
            "Solid red 16x16 Kotlin must match Python byte-for-byte",
            expected,
            encoded
        )
    }

    @Test
    fun solidRedFullPacketsMatchPythonReference() {
        val session = DivoomSession(controllerUuid = "12345678-1234-1234-1234-123456789abc")
        session.seq = 1
        PacketEnvelope.buildPacket("{}".toByteArray(), session)
        session.bumpToAuthenticated()

        val frame = com.divoom.navigationarrow.domain.model.PixelFrame.solid(0xFF0000)
        val inner = StaticImageEncoder.encodeImage(frame)
        val (slot, confirm) = session.nextImageSlot()
        val p0 = PacketEnvelope.buildPacket(
            byteArrayOf(0xBD.toByte(), 0x31, slot.toByte(), 0x01),
            session
        )
        val p1 = PacketEnvelope.buildPacket(
            byteArrayOf(0x9F.toByte(), confirm.toByte()),
            session
        )
        val p2 = PacketEnvelope.buildPacket(inner, session)

        assertArrayEquals(
            hexToBytes("FE EF AA 55 0B 00 01 01 00 00 00 BD 31 00 01 FC 00"),
            p0
        )
        assertArrayEquals(
            hexToBytes("FE EF AA 55 09 00 01 02 00 00 00 9F B1 5C 01"),
            p1
        )
        assertArrayEquals(
            hexToBytes(
                "FE EF AA 55 36 00 01 03 00 00 00 44 00 0A 0A 04 AA 2A 00 F4 01 00 01 FF 00 00 " +
                    "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
                    "00 00 00 00 00 00 00 00 5F 03"
            ),
            p2
        )
    }

    @Test
    fun singleColorHasF40100MysteryBytes() {
        val encoded = StaticImageEncoder.encodeImage(
            com.divoom.navigationarrow.domain.model.PixelFrame.solid(0xFF00FF)
        )
        assertEquals(0x44, encoded[0].toInt() and 0xFF)
        assertEquals(0xF4, encoded[8].toInt() and 0xFF)
        assertEquals(0x01, encoded[9].toInt() and 0xFF)
        assertEquals(0x00, encoded[10].toInt() and 0xFF)
        assertEquals(0x01, encoded[11].toInt() and 0xFF)
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.trim().split(Regex("\\s+")).map { it.toInt(16).toByte() }.toByteArray()
}

class PixelFrameTest {

    @Test
    fun solidHas256Pixels() {
        val f = com.divoom.navigationarrow.domain.model.PixelFrame.solid(0xFF0000)
        assertEquals(16, f.width)
        assertEquals(16, f.height)
        assertEquals(256, f.pixels.size)
        assertEquals(0xFF0000, f.getPixel(0, 0))
        assertEquals(0xFF0000, f.getPixel(15, 15))
    }

    @Test
    fun rowMajorOrder() {
        val pixels = IntArray(256) { 0 }
        pixels[0] = 0xFF0000
        pixels[1] = 0x00FF00
        pixels[16] = 0x0000FF
        val f = com.divoom.navigationarrow.domain.model.PixelFrame(16, 16, pixels)
        assertEquals(0xFF0000, f.getPixel(0, 0))
        assertEquals(0x00FF00, f.getPixel(1, 0))
        assertEquals(0x0000FF, f.getPixel(0, 1))
    }

    @Test
    fun navigationPatternsAreValid16x16Frames() {
        val patterns = listOf(
            com.divoom.navigationarrow.domain.model.TestPatterns.getArrowLeft(),
            com.divoom.navigationarrow.domain.model.TestPatterns.getArrowRight(),
            com.divoom.navigationarrow.domain.model.TestPatterns.getArrowStraight(),
            com.divoom.navigationarrow.domain.model.TestPatterns.getUTurn(),
            com.divoom.navigationarrow.domain.model.TestPatterns.getRoundabout(),
            com.divoom.navigationarrow.domain.model.TestPatterns.getDestination()
        )
        patterns.forEach { frame ->
            assertEquals(16, frame.width)
            assertEquals(16, frame.height)
            assertEquals(256, frame.pixels.size)
            assertTrue(frame.pixels.any { it != 0 })
        }
    }
}

class PacketChunkerTest {

    @Test
    fun chunksAt138() {
        val data = ByteArray(300) { it.toByte() }
        val chunks = com.divoom.navigationarrow.domain.transport.PacketChunker.chunk(data, 138)
        assertEquals(3, chunks.size)
        assertEquals(138, chunks[0].size)
        assertEquals(138, chunks[1].size)
        assertEquals(24, chunks[2].size)
        assertArrayEquals(data, chunks[0] + chunks[1] + chunks[2])
    }

    @Test
    fun smallPacketSingleChunk() {
        val data = byteArrayOf(1, 2, 3)
        val chunks = com.divoom.navigationarrow.domain.transport.PacketChunker.chunk(data, 138)
        assertEquals(1, chunks.size)
        assertArrayEquals(data, chunks[0])
    }
}
