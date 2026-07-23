package com.divoomspeed.backpack.location

import kotlin.math.roundToInt

data class SpeedReading(
    val rawMetersPerSecond: Float = 0f,
    val speedKmh: Float = 0f,
    val displaySpeedKmh: Int = 0,
    val accuracyMeters: Float? = null,
    val hasSpeed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isAccuracyLow: Boolean
        get() = accuracyMeters != null && accuracyMeters > 50f

    val isSignalLost: Boolean
        get() = (System.currentTimeMillis() - timestamp) > 5000L

    val displayText: String
        get() = when {
            isSignalLost -> "--"
            !hasSpeed -> "--"
            else -> displaySpeedKmh.toString()
        }

    companion object {
        fun create(
            metersPerSecond: Float,
            hasSpeed: Boolean,
            accuracy: Float?,
            timestamp: Long = System.currentTimeMillis(),
            previousSmoothedKmh: Float? = null,
            useSmoothing: Boolean = true,
            smoothingAlpha: Float = 0.4f
        ): SpeedReading {
            if (!hasSpeed || metersPerSecond < 0f) {
                return SpeedReading(
                    rawMetersPerSecond = 0f,
                    speedKmh = 0f,
                    displaySpeedKmh = 0,
                    accuracyMeters = accuracy,
                    hasSpeed = false,
                    timestamp = timestamp
                )
            }

            val rawKmh = metersPerSecond * 3.6f

            // EMA Smoothing
            val finalKmh = if (useSmoothing && previousSmoothedKmh != null) {
                (smoothingAlpha * rawKmh) + ((1f - smoothingAlpha) * previousSmoothedKmh)
            } else {
                rawKmh
            }

            // Minimum speed threshold to prevent number flickering at rest
            val displayKmh = if (finalKmh < 2.0f) 0 else finalKmh.roundToInt()

            return SpeedReading(
                rawMetersPerSecond = metersPerSecond,
                speedKmh = finalKmh,
                displaySpeedKmh = displayKmh,
                accuracyMeters = accuracy,
                hasSpeed = true,
                timestamp = timestamp
            )
        }
    }
}
