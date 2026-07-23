package com.divoomspeed.backpack.location

import kotlinx.coroutines.flow.StateFlow

interface SpeedTracker {
    val speedState: StateFlow<SpeedReading>

    suspend fun start(): Result<Unit>

    suspend fun stop()

    fun setSmoothingEnabled(enabled: Boolean)
}
