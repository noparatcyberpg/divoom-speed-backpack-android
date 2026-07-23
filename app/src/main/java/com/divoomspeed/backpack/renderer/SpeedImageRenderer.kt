package com.divoomspeed.backpack.renderer

interface SpeedImageRenderer {
    fun render(speedKmh: Int, config: DisplayConfig): PixelFrame
}

class DefaultSpeedImageRenderer : SpeedImageRenderer {

    companion object {
        const val COLOR_BLACK = 0xFF000000.toInt()
        const val COLOR_GREEN = 0xFF00FF40.toInt()
        const val COLOR_YELLOW = 0xFFFFEB00.toInt()
        const val COLOR_ORANGE = 0xFFFF8000.toInt()
        const val COLOR_RED = 0xFFFF2020.toInt()
        const val COLOR_WHITE = 0xFFFFFFFF.toInt()
        const val COLOR_DARK_GRAY = 0xFF444444.toInt()

        fun rgb(r: Int, g: Int, b: Int): Int {
            return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        }
    }

    override fun render(speedKmh: Int, config: DisplayConfig): PixelFrame {
        val baseWidth = 16
        val baseHeight = 16
        val rawPixels = IntArray(baseWidth * baseHeight) { COLOR_BLACK }

        val textColor = getSpeedColor(speedKmh, config.colorMode)
        val speedStr = speedKmh.coerceAtLeast(0).toString()

        if (speedStr.length <= 2) {
            val d1 = speedStr.getOrNull(0) ?: '0'
            val d2 = speedStr.getOrNull(1)

            if (d2 == null) {
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d1], startX = 5, startY = 2, textColor)
            } else {
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d1], startX = 2, startY = 2, textColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d2], startX = 8, startY = 2, textColor)
            }

            if (config.showUnit) {
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['K'], startX = 2, startY = 11, COLOR_DARK_GRAY)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['M'], startX = 6, startY = 11, COLOR_DARK_GRAY)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['H'], startX = 10, startY = 11, COLOR_DARK_GRAY)
            }
        } else {
            val d1 = speedStr[0]
            val d2 = speedStr[1]
            val d3 = speedStr[2]

            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d1], startX = 2, startY = 3, textColor)
            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d2], startX = 6, startY = 3, textColor)
            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d3], startX = 10, startY = 3, textColor)

            if (config.showUnit) {
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['K'], startX = 2, startY = 11, COLOR_DARK_GRAY)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['M'], startX = 6, startY = 11, COLOR_DARK_GRAY)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['H'], startX = 10, startY = 11, COLOR_DARK_GRAY)
            }
        }

        val rotatedPixels = applyRotation(rawPixels, baseWidth, baseHeight, config.orientation)

        return if (config.width != baseWidth || config.height != baseHeight) {
            scalePixelFrame(PixelFrame(baseWidth, baseHeight, rotatedPixels), config.width, config.height)
        } else {
            PixelFrame(baseWidth, baseHeight, rotatedPixels)
        }
    }

    private fun drawGlyph(
        pixels: IntArray,
        canvasW: Int,
        canvasH: Int,
        glyph: Array<String>?,
        startX: Int,
        startY: Int,
        color: Int
    ) {
        if (glyph == null) return
        for (r in glyph.indices) {
            val line = glyph[r]
            for (c in line.indices) {
                if (line[c] == '1') {
                    val px = startX + c
                    val py = startY + r
                    if (px in 0 until canvasW && py in 0 until canvasH) {
                        pixels[py * canvasW + px] = color
                    }
                }
            }
        }
    }

    fun getSpeedColor(speedKmh: Int, mode: SpeedColorMode): Int {
        return when (mode) {
            SpeedColorMode.SPEED_BASED -> when {
                speedKmh <= 30 -> COLOR_GREEN
                speedKmh <= 60 -> COLOR_YELLOW
                speedKmh <= 90 -> COLOR_ORANGE
                else -> COLOR_RED
            }
            SpeedColorMode.MONOCHROME_GREEN -> COLOR_GREEN
            SpeedColorMode.MONOCHROME_WHITE -> COLOR_WHITE
            SpeedColorMode.RAINBOW -> COLOR_YELLOW
            SpeedColorMode.HIGH_CONTRAST -> COLOR_YELLOW
        }
    }

    private fun applyRotation(
        pixels: IntArray,
        w: Int,
        h: Int,
        orientation: DisplayOrientation
    ): IntArray {
        if (orientation == DisplayOrientation.ROTATION_0) return pixels

        val result = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val oldIdx = y * w + x
                val (nx, ny) = when (orientation) {
                    DisplayOrientation.ROTATION_90 -> Pair(h - 1 - y, x)
                    DisplayOrientation.ROTATION_180 -> Pair(w - 1 - x, h - 1 - y)
                    DisplayOrientation.ROTATION_270 -> Pair(y, w - 1 - x)
                    DisplayOrientation.ROTATION_0 -> Pair(x, y)
                }
                result[ny * w + nx] = pixels[oldIdx]
            }
        }
        return result
    }

    private fun scalePixelFrame(source: PixelFrame, targetW: Int, targetH: Int): PixelFrame {
        val scaled = IntArray(targetW * targetH)
        val scaleX = targetW.toFloat() / source.width
        val scaleY = targetH.toFloat() / source.height

        for (ty in 0 until targetH) {
            for (tx in 0 until targetW) {
                val sx = (tx / scaleX).toInt().coerceIn(0, source.width - 1)
                val sy = (ty / scaleY).toInt().coerceIn(0, source.height - 1)
                scaled[ty * targetW + tx] = source.getPixel(sx, sy)
            }
        }
        return PixelFrame(targetW, targetH, scaled)
    }
}
