package com.divoom.navigationarrow.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MotionState(
    val speedKmh: Int = 0,
    val hasFix: Boolean = false,
    val isStopped: Boolean = false,
    val updatedAt: Long = 0L
)

object MotionStateBus {
    private val _state = MutableStateFlow(MotionState())
    val state: StateFlow<MotionState> = _state.asStateFlow()

    fun publish(value: MotionState) {
        _state.value = value
    }
}
