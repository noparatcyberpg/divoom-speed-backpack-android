package com.divoom.navigationarrow.domain.transport

import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.model.TransportDiagnostics
import kotlinx.coroutines.flow.StateFlow

interface DivoomTransport {
    val connectionState: StateFlow<ConnectionState>
    val diagnostics: StateFlow<TransportDiagnostics>

    suspend fun connect(address: String): Result<Unit>

    suspend fun initializeSession(): Result<Unit>

    suspend fun writePacket(
        packet: ByteArray,
        writeType: WriteType
    ): Result<Unit>

    suspend fun disconnect()
}
