package com.divoom.navigationarrow.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectBackoffTest {

    @Test
    fun `reconnect delay backs off without becoming excessive`() {
        assertEquals(1_000L, reconnectDelayMillis(0))
        assertEquals(2_000L, reconnectDelayMillis(1))
        assertEquals(4_000L, reconnectDelayMillis(2))
        assertEquals(8_000L, reconnectDelayMillis(3))
        assertEquals(15_000L, reconnectDelayMillis(4))
        assertEquals(30_000L, reconnectDelayMillis(5))
        assertEquals(30_000L, reconnectDelayMillis(99))
    }

    @Test
    fun `negative reconnect attempt uses first delay`() {
        assertEquals(1_000L, reconnectDelayMillis(-1))
    }

    @Test
    fun `animation becomes faster as motion speed increases`() {
        val noFix = animationDelayMillis(speedKmh = 0, hasFix = false, isStopped = false)
        val slow = animationDelayMillis(speedKmh = 5, hasFix = true, isStopped = false)
        val city = animationDelayMillis(speedKmh = 30, hasFix = true, isStopped = false)
        val fast = animationDelayMillis(speedKmh = 90, hasFix = true, isStopped = false)

        assertEquals(900L, noFix)
        assertEquals(700L, slow)
        assertEquals(330L, city)
        assertEquals(120L, fast)
    }

    @Test
    fun `stopped cartoon uses a relaxed animation pace`() {
        assertEquals(
            420L,
            animationDelayMillis(speedKmh = 0, hasFix = true, isStopped = true)
        )
    }
}
