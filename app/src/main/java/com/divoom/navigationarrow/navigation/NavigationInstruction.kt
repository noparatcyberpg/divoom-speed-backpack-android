package com.divoom.navigationarrow.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavigationManeuver(
    val patternName: String,
    val thaiLabel: String
) {
    LEFT("Arrow Left", "เลี้ยวซ้าย"),
    RIGHT("Arrow Right", "เลี้ยวขวา"),
    STRAIGHT("Arrow Straight", "ตรงไป"),
    U_TURN("U-Turn", "กลับรถ"),
    ROUNDABOUT("Roundabout", "วงเวียน"),
    DESTINATION("Destination", "ถึงจุดหมาย")
}

data class NavigationInstruction(
    val text: String,
    val maneuver: NavigationManeuver,
    val distanceMeters: Int? = null,
    val receivedAt: Long = System.currentTimeMillis()
) {
    fun shouldDisplayTurnArrow(maxDistanceMeters: Int = 100): Boolean {
        if (maneuver != NavigationManeuver.LEFT && maneuver != NavigationManeuver.RIGHT) {
            return true
        }
        return distanceMeters == null || distanceMeters <= maxDistanceMeters
    }
}

object NavigationInstructionBus {
    private val _latest = MutableStateFlow<NavigationInstruction?>(null)
    val latest: StateFlow<NavigationInstruction?> = _latest.asStateFlow()

    fun publish(instruction: NavigationInstruction) {
        _latest.value = instruction
    }
}

object GoogleMapsInstructionParser {

    fun parse(parts: List<String>): NavigationInstruction? {
        val cleanParts = parts
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val text = cleanParts
            .joinToString(" • ")
        if (text.isBlank()) return null
        val maneuver = parseManeuver(text) ?: return null
        val distanceMeters = cleanParts
            .asSequence()
            .mapNotNull(::parseDistanceMeters)
            .firstOrNull()
        return NavigationInstruction(
            text = text,
            maneuver = maneuver,
            distanceMeters = distanceMeters
        )
    }

    fun parseDistanceMeters(instruction: String): Int? {
        val value = normalizeThaiDigits(instruction.lowercase())
        val match = DISTANCE_PATTERN.find(value) ?: return null
        val amount = match.groupValues[1]
            .replace(",", "")
            .toDoubleOrNull()
            ?: return null
        val unit = match.groupValues[2].trimEnd('.')
        val meters = when (unit) {
            "กม", "กิโลเมตร", "km", "kilometer", "kilometers",
            "kilometre", "kilometres" -> amount * 1_000.0

            "ไมล์", "mi", "mile", "miles" -> amount * 1_609.344
            "ฟุต", "ft", "foot", "feet" -> amount * 0.3048
            else -> amount
        }
        return meters.toInt().coerceAtLeast(0)
    }

    fun parseManeuver(instruction: String): NavigationManeuver? {
        val value = instruction
            .lowercase()
            .replace('–', '-')
            .replace('—', '-')

        return when {
            containsAny(
                value,
                "กลับรถ",
                "ยูเทิร์น",
                "u-turn",
                "u turn",
                "make a u"
            ) -> NavigationManeuver.U_TURN

            containsAny(
                value,
                "ถึงจุดหมาย",
                "ถึงปลายทาง",
                "จุดหมายอยู่",
                "arrived",
                "arrive at",
                "destination is"
            ) -> NavigationManeuver.DESTINATION

            containsAny(
                value,
                "วงเวียน",
                "roundabout",
                "traffic circle"
            ) -> NavigationManeuver.ROUNDABOUT

            containsAny(
                value,
                "เลี้ยวซ้าย",
                "ชิดซ้าย",
                "เบี่ยงซ้าย",
                "ทางออกด้านซ้าย",
                "turn left",
                "keep left",
                "slight left",
                "sharp left",
                "exit on the left"
            ) -> NavigationManeuver.LEFT

            containsAny(
                value,
                "เลี้ยวขวา",
                "ชิดขวา",
                "เบี่ยงขวา",
                "ทางออกด้านขวา",
                "turn right",
                "keep right",
                "slight right",
                "sharp right",
                "exit on the right"
            ) -> NavigationManeuver.RIGHT

            containsAny(
                value,
                "ตรงไป",
                "ขับต่อไป",
                "มุ่งหน้า",
                "ใช้เส้นทาง",
                "continue straight",
                "continue on",
                "head north",
                "head south",
                "head east",
                "head west",
                "merge onto"
            ) -> NavigationManeuver.STRAIGHT

            else -> null
        }
    }

    private fun containsAny(value: String, vararg needles: String): Boolean =
        needles.any { value.contains(it) }

    private fun normalizeThaiDigits(value: String): String =
        buildString(value.length) {
            value.forEach { character ->
                append(
                    when (character) {
                        '๐' -> '0'
                        '๑' -> '1'
                        '๒' -> '2'
                        '๓' -> '3'
                        '๔' -> '4'
                        '๕' -> '5'
                        '๖' -> '6'
                        '๗' -> '7'
                        '๘' -> '8'
                        '๙' -> '9'
                        else -> character
                    }
                )
            }
        }

    private val DISTANCE_PATTERN = Regex(
        """(\d+(?:[.,]\d+)?)\s*(กิโลเมตร|กม\.?|kilometers?|kilometres?|km|ไมล์|miles?|mi|ฟุต|feet|foot|ft|เมตร|ม\.?|meters?|metres?|m)(?![a-z])""",
        RegexOption.IGNORE_CASE
    )
}
