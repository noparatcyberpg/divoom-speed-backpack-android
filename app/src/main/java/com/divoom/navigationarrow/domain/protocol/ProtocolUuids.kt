package com.divoom.navigationarrow.domain.protocol

import java.util.UUID

/**
 * GATT UUIDs from orionparrott/divoom-protocol client.py
 * commit f9b661f8e06a6d40be91c800fed656c7402bd2ac
 *
 * Do not invent alternatives. Confirm against live Inspector dump on device.
 */
object ProtocolUuids {
    val SERVICE: UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
    val WRITE_CHAR: UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3")
    val NOTIFY_CHAR: UUID = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")

    const val SERVICE_STRING = "49535343-fe7d-4ae5-8fa9-9fafd205e455"
    const val WRITE_CHAR_STRING = "49535343-8841-43f4-a8d4-ecbe34729bb3"
    const val NOTIFY_CHAR_STRING = "49535343-1e4d-4bd9-ba61-23c647249616"

    /** App-layer write chunk size matching iOS / reference client (not MTU). */
    const val APP_CHUNK_SIZE = 138

    const val INIT_THROTTLE_MS = 60L
    const val INIT_DEVICE_INFO_PAUSE_MS = 1000L
}
