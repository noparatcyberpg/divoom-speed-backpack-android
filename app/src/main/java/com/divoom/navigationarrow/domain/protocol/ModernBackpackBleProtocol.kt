package com.divoom.navigationarrow.domain.protocol

import com.divoom.navigationarrow.domain.model.PixelFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface DivoomProtocol {
    val name: String
    val initialized: StateFlow<Boolean>

    fun buildInitializationPackets(): List<ByteArray>

    fun encodeStaticImage(frame: PixelFrame): Result<List<ByteArray>>

    fun encodeBrightness(value: Int): Result<List<ByteArray>>

    fun encodeClearScreen(): Result<List<ByteArray>>

    fun encodeLighting(r: Int, g: Int, b: Int, brightness: Int = 100): Result<List<ByteArray>>

    fun resetSession()
}

/**
 * Modern Backpack M / TimeBox-Evo BLE protocol port from orionparrott/divoom-protocol.
 * Not named "Verified" until physical display success is confirmed.
 */
class ModernBackpackBleProtocol(
    controllerUuid: String = java.util.UUID.randomUUID().toString()
) : DivoomProtocol {

    override val name: String = "ModernBackpackBleProtocol (divoom-protocol @ f9b661f)"

    private val session = DivoomSession(controllerUuid = controllerUuid)
    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    override fun resetSession() {
        session.resetForNewConnection()
        _initialized.value = false
    }

    fun markInitialized() {
        _initialized.value = true
    }

    fun sessionForTests(): DivoomSession = session

    /**
     * Builds full init packet list matching client.init_session order.
     * Caller must throttle (~60ms) and pause ~1s before deviceInfo section.
     *
     * Indices for pacing:
     * - After packets[0] (SetUTC) session is already bumped.
     * - Pause before the packet that starts with BA 01 (deviceInfo) — see [deviceInfoPacketIndex].
     */
    override fun buildInitializationPackets(): List<ByteArray> {
        session.resetForNewConnection()
        val packets = mutableListOf<ByteArray>()

        // 1. SetUTC — keystone
        packets.add(buildSetUtc())

        // 2. Binary timesync
        packets.add(buildSyncTime())

        // 3. Long bd / query sequence
        val initPayloads = listOf(
            byteArrayOf(0xBD.toByte(), 0x1F, 0x01),
            byteArrayOf(0x8E.toByte(), 0x00),
            byteArrayOf(0xBD.toByte(), 0x13),
            byteArrayOf(0xBD.toByte(), 0x18),
            byteArrayOf(0xBD.toByte(), 0x15),
            byteArrayOf(0xBD.toByte(), 0x17, 0xFF.toByte()),
            byteArrayOf(0xBD.toByte(), 0x25, 0x00),
            byteArrayOf(0xBD.toByte(), 0x26, 0x00),
            byteArrayOf(0xBD.toByte(), 0x27),
            byteArrayOf(0x46),
            byteArrayOf(0x26, 0xFF.toByte()),
            byteArrayOf(0x15),
            byteArrayOf(0xB6.toByte(), 0xFF.toByte()),
            byteArrayOf(0x97.toByte(), 0x00),
            byteArrayOf(0x36),
            byteArrayOf(0x37, 0x00),
            byteArrayOf(0x8A.toByte(), 0x00),
            byteArrayOf(0xB0.toByte()),
            byteArrayOf(0xA2.toByte()),
            byteArrayOf(0x42),
            byteArrayOf(0x59),
            byteArrayOf(0x76),
        )
        for (p in initPayloads) {
            packets.add(PacketEnvelope.buildPacket(p, session))
        }

        // registerController
        packets.add(buildRegisterController())

        for (p in listOf(
            byteArrayOf(0x76),
            byteArrayOf(0x27, 0x02),
            byteArrayOf(0x5F, 0x15, 0x0B)
        )) {
            packets.add(PacketEnvelope.buildPacket(p, session))
        }

        // Font blob
        packets.add(PacketEnvelope.buildPacket(FontBlob.BYTES, session))

        // Unknown 10-byte near end of first pass
        packets.add(
            PacketEnvelope.buildPacket(
                byteArrayOf(
                    0x9B.toByte(), 0x01, 0x4E, 0x6F, 0x62, 0x02, 0x41, 0x04, 0x00, 0x00
                ),
                session
            )
        )

        // --- PAUSE here (~1s) before deviceInfo — caller handles ---

        // Second pass
        packets.add(PacketEnvelope.buildPacket(byteArrayOf(0xBA.toByte(), 0x01), session))
        packets.add(buildRegisterController())
        packets.add(PacketEnvelope.buildPacket(byteArrayOf(0x76), session))
        packets.add(PacketEnvelope.buildPacket(byteArrayOf(0x27, 0x02), session))

        // Session unlock — NEVER re-send after init
        packets.add(PacketEnvelope.buildPacket(byteArrayOf(0xBD.toByte(), 0x2F, 0x02), session))
        packets.add(PacketEnvelope.buildPacket(byteArrayOf(0x31), session))

        return packets
    }

    /** Index of BA 01 deviceInfo packet within [buildInitializationPackets] result. */
    fun deviceInfoPacketIndex(): Int {
        // Count: SetUTC(1)+sync(1)+22 init + register(1)+3 + font(1)+10byte(1) = 30
        // deviceInfo is next → index 30
        return 30
    }

    override fun encodeStaticImage(frame: PixelFrame): Result<List<ByteArray>> {
        return try {
            if (!_initialized.value && !session.isAuthenticated()) {
                return Result.failure(IllegalStateException("Session initialization failed / not initialized"))
            }
            val inner = StaticImageEncoder.encodeImage(frame)
            val (slot, confirm) = session.nextImageSlot()
            val preamble1 = PacketEnvelope.buildPacket(
                byteArrayOf(0xBD.toByte(), 0x31, (slot and 0xFF).toByte(), 0x01),
                session
            )
            val preamble2 = PacketEnvelope.buildPacket(
                byteArrayOf(0x9F.toByte(), (confirm and 0xFF).toByte()),
                session
            )
            val imagePkt = PacketEnvelope.buildPacket(inner, session)
            Result.success(listOf(preamble1, preamble2, imagePkt))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Packet encoding failed: ${e.message}", e))
        }
    }

    override fun encodeBrightness(value: Int): Result<List<ByteArray>> {
        return try {
            val v = value.coerceIn(0, 100)
            val pkt = PacketEnvelope.buildPacket(byteArrayOf(0x74, v.toByte()), session)
            Result.success(listOf(pkt))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun encodeClearScreen(): Result<List<ByteArray>> =
        encodeStaticImage(PixelFrame.clear())

    override fun encodeLighting(r: Int, g: Int, b: Int, brightness: Int): Result<List<ByteArray>> {
        return try {
            val bri = brightness.coerceIn(0, 100)
            val payload = byteArrayOf(
                0x45,
                0x01,
                (r and 0xFF).toByte(),
                (g and 0xFF).toByte(),
                (b and 0xFF).toByte(),
                bri.toByte(),
                0x00,
                0x01
            )
            Result.success(listOf(PacketEnvelope.buildPacket(payload, session)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSetUtc(): ByteArray {
        val nowSec = System.currentTimeMillis() / 1000L
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        val timeStr = sdf.format(Date())
        // Match iOS: escape forward slash in Command value
        val json =
            "{\"Command\":\"Device\\/SetUTC\",\"Utc\":$nowSec,\"Time\":\"$timeStr\"}"
        val payload = json.toByteArray(Charsets.UTF_8)
        val packet = PacketEnvelope.buildPacket(payload, session)
        session.bumpToAuthenticated()
        return packet
    }

    private fun buildSyncTime(): ByteArray {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR) % 100
        // ISO week
        cal.firstDayOfWeek = java.util.Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        val isoWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val second = cal.get(java.util.Calendar.SECOND)
        // Python: weekday Sun=0..Sat=6 ; Calendar.DAY_OF_WEEK Sun=1..Sat=7
        val weekday = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
        val payload = byteArrayOf(
            0x18,
            year.toByte(),
            isoWeek.toByte(),
            month.toByte(),
            day.toByte(),
            hour.toByte(),
            minute.toByte(),
            second.toByte(),
            weekday.toByte()
        )
        return PacketEnvelope.buildPacket(payload, session)
    }

    private fun buildRegisterController(): ByteArray {
        val raw = session.controllerUuid.replace("-", "").uppercase(Locale.US)
        val snippet = (raw.take(8) + "-" + raw.drop(8).take(4) + "-" + raw.drop(12).take(2)).take(16)
        val payload = byteArrayOf(0x4F) + snippet.toByteArray(Charsets.US_ASCII)
        return PacketEnvelope.buildPacket(payload, session)
    }
}
