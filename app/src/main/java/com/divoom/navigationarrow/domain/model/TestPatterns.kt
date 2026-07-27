package com.divoom.navigationarrow.domain.model

object TestPatterns {
    val RED = 0xFF0000
    val GREEN = 0x00FF00
    val BLUE = 0x0000FF
    val WHITE = 0xFFFFFF
    val BLACK = 0x000000
    val YELLOW = 0xFFFF00
    val GRAY = 0x808080
    val CYAN = 0x45E6FF
    val PINK = 0xFF69B4

    fun getSolidRed() = PixelFrame.solid(RED)
    fun getSolidGreen() = PixelFrame.solid(GREEN)
    fun getSolidBlue() = PixelFrame.solid(BLUE)
    fun getSolidWhite() = PixelFrame.solid(WHITE)
    fun getSolidBlack() = PixelFrame.solid(BLACK)
    fun getClear() = PixelFrame.clear()

    fun getCheckerboard(): PixelFrame {
        val pixels = IntArray(256)
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                pixels[y * 16 + x] = if ((x + y) % 2 == 0) WHITE else BLACK
            }
        }
        return PixelFrame(16, 16, pixels)
    }

    fun getHorizontalGradient(): PixelFrame {
        val pixels = IntArray(256)
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val v = (x * 255 / 15).coerceIn(0, 255)
                pixels[y * 16 + x] = (v shl 16) or (v shl 8) or v
            }
        }
        return PixelFrame(16, 16, pixels)
    }

    fun getArrowLeft(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000000000000000",
            "0000011000000000",
            "0000111000000000",
            "0001111000000000",
            "0011111000000000",
            "0111111111111100",
            "1111111111111100",
            "1111111111111100",
            "0111111111111100",
            "0011111000000000",
            "0001111000000000",
            "0000111000000000",
            "0000011000000000",
            "0000000000000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to WHITE))
    }

    fun getArrowRight(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000000000000000",
            "0000000001100000",
            "0000000001110000",
            "0000000001111000",
            "0000000001111100",
            "0011111111111110",
            "0011111111111111",
            "0011111111111111",
            "0011111111111110",
            "0000000001111100",
            "0000000001111000",
            "0000000001110000",
            "0000000001100000",
            "0000000000000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to WHITE))
    }

    fun getArrowStraight(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000000110000000",
            "0000001111000000",
            "0000011111100000",
            "0000111111110000",
            "0001111111111000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000001111000000",
            "0000000000000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to WHITE))
    }

    fun getUTurn(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000011111100000",
            "0001111111111000",
            "0011110000111100",
            "0111100000011110",
            "0111000000001110",
            "0111000000001110",
            "0111000000001110",
            "0111000000001110",
            "0111000000001110",
            "0111111111001110",
            "0111111111101110",
            "0000001111001110",
            "0000011110001110",
            "0000111100001110",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to YELLOW))
    }

    fun getDestination(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000111111100000",
            "0000111111110000",
            "0000111111111000",
            "0000111111110000",
            "0000111111100000",
            "0000100000000000",
            "0000100000000000",
            "0000100000000000",
            "0000100000000000",
            "0000100000000000",
            "0000100000000000",
            "0000100000000000",
            "0001111100000000",
            "0011111110000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to GREEN))
    }

    fun getRoundabout(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000011111000000",
            "0001111111110000",
            "0011100000111000",
            "0111000000011100",
            "0110000000001100",
            "1100000000000110",
            "1100000000000110",
            "1100000000000110",
            "0110000000001110",
            "0111000000011110",
            "0011100000111110",
            "0001111111111110",
            "0000011111001110",
            "0000000000000110",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to YELLOW))
    }

    fun getWaitingHeart(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0000000000000000",
            "0000110011000000",
            "0001111111100000",
            "0011111111110000",
            "0011111111110000",
            "0011111111110000",
            "0001111111100000",
            "0000111111000000",
            "0000011110000000",
            "0000001100000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to PINK))
    }

    /** Simple "50" digit art on 16×16 */
    fun getNumber50(): PixelFrame {
        val pattern = arrayOf(
            "0000000000000000",
            "0111100011111000",
            "1000010100000100",
            "1000000100000100",
            "1000000100000100",
            "0111100100000100",
            "0000010100000100",
            "0000010100000100",
            "0000010100000100",
            "1000010100000100",
            "0111100011111000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
        )
        return fillPattern(pattern, mapOf('1' to YELLOW))
    }

    /** Two brief sparkle frames followed by the clean symbol. */
    fun getCuteAnimationFrames(frame: PixelFrame): List<PixelFrame> {
        require(frame.width == 16 && frame.height == 16) {
            "Navigation animation requires a 16×16 frame"
        }
        val first = addSparkles(
            frame,
            listOf(
                Sparkle(2, 2, CYAN, large = true),
                Sparkle(13, 12, PINK, large = false)
            )
        )
        val second = addSparkles(
            frame,
            listOf(
                Sparkle(2, 2, PINK, large = false),
                Sparkle(13, 3, YELLOW, large = true),
                Sparkle(3, 13, CYAN, large = false)
            )
        )
        return listOf(first, second, frame)
    }

    private fun fillPattern(pattern: Array<String>, colorMap: Map<Char, Int>): PixelFrame {
        val pixels = IntArray(256)
        for (y in 0 until 16) {
            val row = pattern[y]
            for (x in 0 until 16) {
                pixels[y * 16 + x] = colorMap[row[x]] ?: BLACK
            }
        }
        return PixelFrame(16, 16, pixels)
    }

    private data class Sparkle(
        val x: Int,
        val y: Int,
        val color: Int,
        val large: Boolean
    )

    private fun addSparkles(frame: PixelFrame, sparkles: List<Sparkle>): PixelFrame {
        val pixels = frame.pixels.copyOf()

        fun lightPixel(x: Int, y: Int, color: Int) {
            if (x !in 0 until 16 || y !in 0 until 16) return
            val index = y * 16 + x
            if ((pixels[index] and 0xFFFFFF) == BLACK) {
                pixels[index] = color
            }
        }

        sparkles.forEach { sparkle ->
            lightPixel(sparkle.x, sparkle.y, sparkle.color)
            if (sparkle.large) {
                lightPixel(sparkle.x - 1, sparkle.y, sparkle.color)
                lightPixel(sparkle.x + 1, sparkle.y, sparkle.color)
                lightPixel(sparkle.x, sparkle.y - 1, sparkle.color)
                lightPixel(sparkle.x, sparkle.y + 1, sparkle.color)
            }
        }
        return PixelFrame(16, 16, pixels)
    }
}
