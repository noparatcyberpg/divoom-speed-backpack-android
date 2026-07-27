package com.divoom.navigationarrow.domain.model

import kotlin.random.Random

data class StoppedCartoonAnimation(
    val name: String,
    val frames: List<PixelFrame>
)

/**
 * Small, high-contrast cartoons designed specifically for the backpack's 16x16 display.
 * No text frames are included here.
 */
object StoppedCartoons {
    private const val ORANGE = 0xFF9F43
    private const val BROWN = 0x8A4F22
    private const val LIME = 0x5DFF72

    val all: List<StoppedCartoonAnimation> by lazy {
        listOf(
            StoppedCartoonAnimation(
                name = "Cheeky face",
                frames = listOf(
                    cheekyFace(wink = false, tongueOut = false),
                    cheekyFace(wink = true, tongueOut = false),
                    cheekyFace(wink = true, tongueOut = true),
                    cheekyFace(wink = false, tongueOut = true),
                    cheekyFace(wink = true, tongueOut = true),
                    cheekyFace(wink = false, tongueOut = false)
                )
            ),
            StoppedCartoonAnimation(
                name = "Dancing chick",
                frames = listOf(
                    dancingChick(dx = -1, dy = 0, wingsUp = false),
                    dancingChick(dx = 0, dy = -1, wingsUp = true),
                    dancingChick(dx = 1, dy = 0, wingsUp = false),
                    dancingChick(dx = 0, dy = -1, wingsUp = true),
                    dancingChick(dx = -1, dy = 0, wingsUp = false),
                    dancingChick(dx = 0, dy = 0, wingsUp = true)
                )
            ),
            StoppedCartoonAnimation(
                name = "Waving cat",
                frames = listOf(
                    wavingCat(waveLeft = true, bobUp = false),
                    wavingCat(waveLeft = true, bobUp = true),
                    wavingCat(waveLeft = false, bobUp = false),
                    wavingCat(waveLeft = false, bobUp = true),
                    wavingCat(waveLeft = true, bobUp = false),
                    wavingCat(waveLeft = false, bobUp = false)
                )
            ),
            StoppedCartoonAnimation(
                name = "Silly frog",
                frames = listOf(
                    sillyFrog(dy = 0, blink = false, tongueOut = false),
                    sillyFrog(dy = -1, blink = false, tongueOut = false),
                    sillyFrog(dy = 0, blink = true, tongueOut = false),
                    sillyFrog(dy = 0, blink = false, tongueOut = true),
                    sillyFrog(dy = -1, blink = false, tongueOut = true),
                    sillyFrog(dy = 0, blink = false, tongueOut = false)
                )
            )
        )
    }

    fun nextIndex(current: Int?, randomValue: Int = Random.nextInt()): Int {
        val candidates = all.indices.filterNot { it == current }.ifEmpty { all.indices.toList() }
        return candidates[Math.floorMod(randomValue, candidates.size)]
    }

    private fun cheekyFace(wink: Boolean, tongueOut: Boolean): PixelFrame =
        frame {
            fill(5, 10, 2, TestPatterns.YELLOW)
            fill(3, 12, 3, TestPatterns.YELLOW)
            fill(2, 13, 4, TestPatterns.YELLOW)
            fill(1, 14, 5, TestPatterns.YELLOW)
            fill(1, 14, 6, TestPatterns.YELLOW)
            fill(1, 14, 7, TestPatterns.YELLOW)
            fill(1, 14, 8, TestPatterns.YELLOW)
            fill(1, 14, 9, TestPatterns.YELLOW)
            fill(1, 14, 10, TestPatterns.YELLOW)
            fill(2, 13, 11, TestPatterns.YELLOW)
            fill(3, 12, 12, TestPatterns.YELLOW)
            fill(5, 10, 13, TestPatterns.YELLOW)

            if (wink) {
                fill(3, 6, 7, TestPatterns.BLACK)
            } else {
                rect(3, 5, 5, 7, TestPatterns.WHITE)
                pixel(4, 6, TestPatterns.BLUE)
            }
            rect(10, 5, 12, 7, TestPatterns.WHITE)
            pixel(11, 6, TestPatterns.BLUE)
            pixel(3, 9, TestPatterns.PINK)
            pixel(13, 9, TestPatterns.PINK)
            fill(5, 10, 9, TestPatterns.BLACK)
            fill(6, 9, 10, TestPatterns.BLACK)
            if (tongueOut) {
                fill(7, 9, 11, TestPatterns.PINK)
                pixel(8, 12, TestPatterns.PINK)
            } else {
                fill(7, 8, 10, TestPatterns.WHITE)
            }
        }

    private fun dancingChick(dx: Int, dy: Int, wingsUp: Boolean): PixelFrame =
        frame {
            fun p(x: Int, y: Int, color: Int) = pixel(x + dx, y + dy, color)
            fun r(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
                rect(left + dx, top + dy, right + dx, bottom + dy, color)

            r(5, 3, 10, 3, TestPatterns.YELLOW)
            r(3, 4, 12, 5, TestPatterns.YELLOW)
            r(2, 6, 13, 9, TestPatterns.YELLOW)
            r(3, 10, 12, 11, TestPatterns.YELLOW)
            r(5, 12, 10, 12, TestPatterns.YELLOW)
            r(4, 6, 6, 7, TestPatterns.WHITE)
            r(9, 6, 11, 7, TestPatterns.WHITE)
            p(5, 7, TestPatterns.BLACK)
            p(10, 7, TestPatterns.BLACK)
            r(12, 8, 14, 8, ORANGE)
            p(13, 9, ORANGE)
            if (wingsUp) {
                r(0, 5, 2, 6, TestPatterns.YELLOW)
                r(13, 4, 14, 6, TestPatterns.YELLOW)
            } else {
                r(1, 9, 3, 11, TestPatterns.YELLOW)
                r(12, 9, 14, 11, TestPatterns.YELLOW)
            }
            r(5, 13, 5, 14, ORANGE)
            r(10, 13, 10, 14, ORANGE)
            r(4, 14, 6, 14, ORANGE)
            r(9, 14, 11, 14, ORANGE)
        }

    private fun wavingCat(waveLeft: Boolean, bobUp: Boolean): PixelFrame =
        frame {
            val dy = if (bobUp) -1 else 0
            fun p(x: Int, y: Int, color: Int) = pixel(x, y + dy, color)
            fun r(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
                rect(left, top + dy, right, bottom + dy, color)

            p(3, 2, ORANGE)
            r(3, 3, 5, 5, ORANGE)
            p(4, 2, ORANGE)
            p(12, 2, ORANGE)
            r(10, 3, 12, 5, ORANGE)
            p(11, 2, ORANGE)
            r(4, 4, 11, 9, ORANGE)
            r(5, 10, 10, 13, ORANGE)
            r(5, 6, 6, 7, TestPatterns.WHITE)
            r(9, 6, 10, 7, TestPatterns.WHITE)
            p(6, 7, TestPatterns.GREEN)
            p(9, 7, TestPatterns.GREEN)
            p(8, 8, TestPatterns.PINK)
            p(7, 9, TestPatterns.WHITE)
            p(9, 9, TestPatterns.WHITE)
            r(12, 11, 14, 12, ORANGE)
            p(14, 10, ORANGE)
            r(5, 14, 6, 14, TestPatterns.WHITE)
            r(9, 14, 10, 14, TestPatterns.WHITE)
            if (waveLeft) {
                r(2, 7, 3, 10, ORANGE)
                r(1, 6, 2, 7, TestPatterns.WHITE)
                r(11, 10, 12, 12, ORANGE)
            } else {
                r(3, 10, 4, 12, ORANGE)
                r(12, 7, 13, 10, ORANGE)
                r(13, 6, 14, 7, TestPatterns.WHITE)
            }
        }

    private fun sillyFrog(dy: Int, blink: Boolean, tongueOut: Boolean): PixelFrame =
        frame {
            fun p(x: Int, y: Int, color: Int) = pixel(x, y + dy, color)
            fun r(left: Int, top: Int, right: Int, bottom: Int, color: Int) =
                rect(left, top + dy, right, bottom + dy, color)

            r(3, 2, 6, 5, LIME)
            r(9, 2, 12, 5, LIME)
            r(2, 5, 13, 11, LIME)
            r(3, 12, 12, 12, LIME)
            if (blink) {
                r(4, 4, 6, 4, TestPatterns.BLACK)
                r(9, 4, 11, 4, TestPatterns.BLACK)
            } else {
                r(4, 3, 6, 5, TestPatterns.WHITE)
                r(9, 3, 11, 5, TestPatterns.WHITE)
                p(5, 4, TestPatterns.BLACK)
                p(10, 4, TestPatterns.BLACK)
            }
            p(3, 9, TestPatterns.PINK)
            p(12, 9, TestPatterns.PINK)
            r(5, 9, 10, 9, TestPatterns.BLACK)
            if (tongueOut) {
                r(7, 10, 8, 13, TestPatterns.PINK)
                r(6, 13, 9, 13, TestPatterns.PINK)
            } else {
                p(7, 10, TestPatterns.WHITE)
                p(8, 10, TestPatterns.WHITE)
            }
            r(1, 12, 4, 13, LIME)
            r(11, 12, 14, 13, LIME)
        }

    private inline fun frame(draw: FrameCanvas.() -> Unit): PixelFrame =
        FrameCanvas().apply(draw).build()

    private class FrameCanvas {
        private val pixels = IntArray(16 * 16)

        fun pixel(x: Int, y: Int, color: Int) {
            if (x in 0 until 16 && y in 0 until 16) {
                pixels[y * 16 + x] = color and 0xFFFFFF
            }
        }

        fun fill(left: Int, right: Int, y: Int, color: Int) {
            for (x in left..right) pixel(x, y, color)
        }

        fun rect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
            for (y in top..bottom) {
                fill(left, right, y, color)
            }
        }

        fun build(): PixelFrame = PixelFrame(16, 16, pixels)
    }
}
