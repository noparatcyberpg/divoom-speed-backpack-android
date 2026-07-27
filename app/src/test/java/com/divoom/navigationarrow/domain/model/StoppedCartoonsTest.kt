package com.divoom.navigationarrow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoppedCartoonsTest {

    @Test
    fun `catalog contains several animated cartoons and no blank frames`() {
        assertTrue(StoppedCartoons.all.size >= 4)
        StoppedCartoons.all.forEach { cartoon ->
            assertTrue(cartoon.frames.size >= 4)
            assertTrue(cartoon.frames.distinct().size >= 2)
            cartoon.frames.forEach { frame ->
                assertEquals(16, frame.width)
                assertEquals(16, frame.height)
                assertTrue(frame.pixels.any { it != TestPatterns.BLACK })
            }
        }
    }

    @Test
    fun `next cartoon never immediately repeats`() {
        StoppedCartoons.all.indices.forEach { current ->
            repeat(20) { randomValue ->
                assertNotEquals(
                    current,
                    StoppedCartoons.nextIndex(current, randomValue)
                )
            }
        }
    }

    @Test
    fun `random extremes always select a valid cartoon`() {
        listOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE).forEach { randomValue ->
            assertTrue(
                StoppedCartoons.nextIndex(current = null, randomValue) in
                    StoppedCartoons.all.indices
            )
        }
    }
}
