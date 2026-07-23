package com.divoomspeed.backpack.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FakeSpeedTracker : SpeedTracker {

    private val _speedState = MutableStateFlow(SpeedReading())
    override val speedState: StateFlow<SpeedReading> = _speedState.asStateFlow()

    private var simulationJob: Job? = null
    private var isSimulating = false

    override fun setSmoothingEnabled(enabled: Boolean) {
        // No-op for fake
    }

    override suspend fun start(): Result<Unit> {
        isSimulating = true
        return Result.success(Unit)
    }

    override suspend fun stop() {
        isSimulating = false
        simulationJob?.cancel()
    }

    fun setSpeed(speedKmh: Int) {
        val mps = speedKmh / 3.6f
        _speedState.value = SpeedReading.create(
            metersPerSecond = mps,
            hasSpeed = true,
            accuracy = 5.0f,
            useSmoothing = false
        )
    }

    fun startAutoDemo(scope: CoroutineScope) {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            var current = 0
            var ascending = true
            while (isSimulating) {
                setSpeed(current)
                delay(1000L)
                if (ascending) {
                    current += 5
                    if (current >= 120) {
                        current = 120
                        ascending = false
                    }
                } else {
                    current -= 5
                    if (current <= 0) {
                        current = 0
                        ascending = true
                    }
                }
            }
        }
    }
}
