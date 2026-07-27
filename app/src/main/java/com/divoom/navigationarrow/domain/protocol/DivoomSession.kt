package com.divoom.navigationarrow.domain.protocol

import java.util.UUID

/**
 * Port of divoom_protocol.session.DivoomSession
 * commit f9b661f8e06a6d40be91c800fed656c7402bd2ac
 *
 * Seq high byte is the session authentication token after SetUTC.
 */
class DivoomSession(
    var seq: Int = 1,
    var slot: Int = 0x00,
    var controllerUuid: String = UUID.randomUUID().toString()
) {
    private var authenticated: Boolean = false

    fun nextSeq(): Int {
        val current = seq and 0xFFFF
        seq = (seq + 1) and 0xFFFF
        return current
    }

    /**
     * Call EXACTLY ONCE after building SetUTC. Jumps seq to 0x0101.
     * Without this, display commands are silently ignored.
     */
    fun bumpToAuthenticated() {
        if (authenticated) return
        seq = 0x0101
        authenticated = true
    }

    fun isAuthenticated(): Boolean = authenticated

    fun resetForNewConnection() {
        seq = 1
        slot = 0x00
        authenticated = false
    }

    /**
     * Returns (slot, confirm) for image preamble bd 31 SLOT 01 / 9f CONFIRM.
     * Slot increments by 3; confirm = slot + 0xB1.
     */
    fun nextImageSlot(): Pair<Int, Int> {
        val current = slot and 0xFF
        slot = (slot + 3) and 0xFF
        val confirm = (current + 0xB1) and 0xFF
        return current to confirm
    }
}
