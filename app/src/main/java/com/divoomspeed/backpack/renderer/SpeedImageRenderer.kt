package com.divoomspeed.backpack.renderer

import android.graphics.Color

interface SpeedImageRenderer {
    fun render(speedKmh: Int, config: DisplayConfig): PixelFrame
}

class DefaultSpeedImageRenderer : SpeedImageRenderer {

    override fun render(speedKmh: Int, config: DisplayConfig): PixelFrame {
        val baseWidth = 16
        val baseHeight = 16
        val rawPixels = IntArray(baseWidth * baseHeight) { Color.BLACK }

        val textColor = getSpeedColor(speedKmh, config.colorMode)
        val speedStr = speedKmh.coerceAtLeast(0).toString()

        if (speedStr.length <= 2) {
            // Draw 2 digits using 5x7 Font
            val d1 = speedStr.getOrNull(0) ?: '0'
            val d2 = speedStr.getOrNull(1)

            if (d2 == null) {
                // Single digit centered
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d1], startX = 5, startY = 2, textColor)
            } else {
                // Two digits side-by-side (5px + 1px gap + 5px = 11px wide, starting at x=2)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d1], startX = 2, startY = 2, textColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_5X7[d2], startX = 8, startY = 2, textColor)
            }

            // Draw "KMH" unit at bottom (y=11)
            if (config.showUnit) {
                val unitColor = Color.DKGRAY
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['K'], startX = 2, startY = 11, unitColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['M'], startX = 6, startY = 11, unitColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['H'], startX = 10, startY = 11, unitColor)
            }
        } else {
            // 3 digits (100-199) using 3x5 compact Font
            val d1 = speedStr[0]
            val d2 = speedStr[1]
            val d3 = speedStr[2]

            // 3px + 1px gap + 3px + 1px gap + 3px = 11px wide (starting x=2, y=3)
            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d1], startX = 2, startY = 3, textColor)
            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d2], startX = 6, startY = 3, textColor)
            drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.DIGIT_3X5[d3], startX = 10, startY = 3, textColor)

            if (config.showUnit) {
                val unitColor = Color.DKGRAY
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['K'], startX = 2, startY = 11, unitColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['M'], startX = 6, startY = 11, unitColor)
                drawGlyph(rawPixels, baseWidth, baseHeight, PixelFont.UNIT_KMH_3X3['H'], startX = 10, startY = 11, unitColor)
            }
        }

        // Apply rotation
        val rotatedPixels = applyRotation(rawPixels, baseWidth, baseHeight, config.orientation)

        // Scale to target resolution if width/height > 16
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
                speedKmh <= 30 -> Color.rgb(0, 255, 64)   // Green
                speedKmh <= 60 -> Color.rgb(255, 235, 0)  // Yellow
                speedKmh <= 90 -> Color.rgb(255, 128, 0)  // Orange
                else -> Color.rgb(255, 32, 32)            // Red
            }
            SpeedColorMode.MONOCHROME_GREEN -> Color.GREEN
            SpeedColorMode.MONOCHROME_WHITE -> Color.WHITE
            SpeedColorMode.RAINBOW -> {
                val hue = (speedKmh * 3) % 360
                Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
            }
            SpeedColorMode.HIGH_CONTRAST -> Color.YELLOW
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
