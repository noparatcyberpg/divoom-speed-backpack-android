package com.divoomspeed.backpack

import com.divoomspeed.backpack.location.SpeedReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedReadingTest {

    @Test
    fun testMetersPerSecondToKmHourConversion() {
        val mps = 10.0f // 10 m/s = 36 km/h
        val reading = SpeedReading.create(
            metersPerSecond = mps,
            hasSpeed = true,
            accuracy = 5.0f,
            useSmoothing = false
        )
        assertEquals(36.0f, reading.speedKmh, 0.01f)
        assertEquals(36, reading.displaySpeedKmh)
        assertEquals("36", reading.displayText)
    }

    @Test
    fun testMinimumSpeedThresholdFiltering() {
        val lowMps = 0.4f // 0.4 m/s = 1.44 km/h < 2.0 km/h threshold
        val reading = SpeedReading.create(
            metersPerSecond = lowMps,
            hasSpeed = true,
            accuracy = 5.0f,
            useSmoothing = false
        )
        assertEquals(0, reading.displaySpeedKmh)
    }

    @Test
    fun testEmaSmoothingLogic() {
        val initialSpeed = 10.0f * 3.6f // 36 km/h
        val newRawSpeedMps = 15.0f // 54 km/h raw
        val reading = SpeedReading.create(
            metersPerSecond = newRawSpeedMps,
            hasSpeed = true,
            accuracy = 5.0f,
            previousSmoothedKmh = initialSpeed,
            useSmoothing = true,
            smoothingAlpha = 0.4f
        )
        // Expected: (0.4 * 54) + (0.6 * 36) = 21.6 + 21.6 = 43.2 km/h
        assertEquals(43.2f, reading.speedKmh, 0.1f)
        assertEquals(43, reading.displaySpeedKmh)
    }

    @Test
    fun testNoSpeedOrSignalLostState() {
        val noSpeedReading = SpeedReading.create(
            metersPerSecond = 0f,
            hasSpeed = false,
            accuracy = 100.0f,
            useSmoothing = false
        )
        assertEquals("--", noSpeedReading.displayText)
        assertTrue(noSpeedReading.isAccuracyLow)
    }
}
