package com.divoom.navigationarrow.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR,
    TX,
    RX
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val hexData: String? = null,
    val uuid: String? = null,
    val level: LogLevel = LogLevel.INFO
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toFormattedString(): String {
        val hexStr = if (!hexData.isNullOrBlank()) " | HEX: $hexData" else ""
        val uuidStr = if (!uuid.isNullOrBlank()) " | UUID: $uuid" else ""
        return "[$formattedTime] [${level.name}] [$tag]$uuidStr: $message$hexStr"
    }
}
