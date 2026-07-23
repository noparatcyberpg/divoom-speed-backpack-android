package com.divoomspeed.backpack.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class DebugLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: String,
    val message: String,
    val packetHex: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

    fun toTxtLine(): String {
        val hexPart = if (packetHex != null) " [HEX: $packetHex]" else ""
        return "[$formattedTime] [${level.name}] [$category] $message$hexPart"
    }
}

object DebugLogger {
    private const val MAX_LOG_SIZE = 1000

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun log(level: LogLevel, category: String, message: String, packetHex: String? = null) {
        val anonymizedMsg = anonymizeSensitiveInfo(message)
        val entry = DebugLogEntry(
            level = level,
            category = category,
            message = anonymizedMsg,
            packetHex = packetHex
        )
        val currentList = _logs.value.toMutableList()
        if (currentList.size >= MAX_LOG_SIZE) {
            currentList.removeAt(0)
        }
        currentList.add(entry)
        _logs.value = currentList
    }

    fun d(category: String, message: String, packetHex: String? = null) =
        log(LogLevel.DEBUG, category, message, packetHex)

    fun i(category: String, message: String, packetHex: String? = null) =
        log(LogLevel.INFO, category, message, packetHex)

    fun w(category: String, message: String, packetHex: String? = null) =
        log(LogLevel.WARN, category, message, packetHex)

    fun e(category: String, message: String, packetHex: String? = null) =
        log(LogLevel.ERROR, category, message, packetHex)

    fun clear() {
        _logs.value = emptyList()
    }

    fun anonymizeSensitiveInfo(input: String): String {
        var text = input
        // Mask MAC addresses (AA:BB:CC:DD:EE:FF -> AA:BB:CC:**:**:FF)
        val macRegex = Regex("([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:)[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:([0-9A-Fa-f]{2}:[0-9A-Fa-f]{2})")
        text = macRegex.replace(text, "$1**:**:$2")

        // Exclude global rules sensitive test items
        text = text.replace("trindd@outlook.co.th", "[EXCLUDED_EMAIL]")
        text = text.replace("+66962987415", "[EXCLUDED_PHONE]")
        text = text.replace("0962987415", "[EXCLUDED_PHONE]")

        return text
    }

    fun exportAsTxt(): String {
        return _logs.value.joinToString("\n") { it.toTxtLine() }
    }

    fun exportAsJson(): String {
        val builder = StringBuilder("[\n")
        _logs.value.forEachIndexed { index, entry ->
            builder.append("  {\n")
            builder.append("    \"timestamp\": ${entry.timestamp},\n")
            builder.append("    \"time\": \"${entry.formattedTime}\",\n")
            builder.append("    \"level\": \"${entry.level.name}\",\n")
            builder.append("    \"category\": \"${entry.category}\",\n")
            builder.append("    \"message\": \"${entry.message.replace("\"", "\\\"")}\"")
            if (entry.packetHex != null) {
                builder.append(",\n    \"packetHex\": \"${entry.packetHex}\"\n")
            } else {
                builder.append("\n")
            }
            builder.append("  }")
            if (index < _logs.value.size - 1) builder.append(",")
            builder.append("\n")
        }
        builder.append("]")
        return builder.toString()
    }
}
