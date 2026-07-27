package com.divoom.navigationarrow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TestPatternsAnimationTest {

    @Test
    fun `cute animation keeps the clean symbol as its final frame`() {
        val base = TestPatterns.getArrowStraight()
        val frames = TestPatterns.getCuteAnimationFrames(base)

        assertEquals(3, frames.size)
        assertEquals(base, frames.last())
    }

    @Test
    fun `sparkle frames do not alter navigation symbol pixels`() {
        val base = TestPatterns.getArrowLeft()
        val frames = TestPatterns.getCuteAnimationFrames(base)

        for (y in 0 until 16) {
            for (x in 0 until 16) {
                if (base.getPixel(x, y) != TestPatterns.BLACK) {
                    assertEquals(base.getPixel(x, y), frames[0].getPixel(x, y))
                    assertEquals(base.getPixel(x, y), frames[1].getPixel(x, y))
                }
            }
        }
    }

    @Test
    fun `sparkle phases are visible and different`() {
        val base = TestPatterns.getArrowRight()
        val frames = TestPatterns.getCuteAnimationFrames(base)

        assertNotEquals(base, frames[0])
        assertNotEquals(base, frames[1])
        assertNotEquals(frames[0], frames[1])
    }
}
