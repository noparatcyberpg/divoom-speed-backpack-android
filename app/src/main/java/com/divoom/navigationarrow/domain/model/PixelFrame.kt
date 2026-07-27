package com.divoom.navigationarrow.domain.model

/**
 * 16×16 (or other) RGB frame. Pixels are 0x00RRGGBB, row-major (left→right, top→bottom).
 * Order matches orionparrott/divoom-protocol encoder.
 */
data class PixelFrame(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(pixels.size == width * height) {
            "pixels size ${pixels.size} != width*height ${width * height}"
        }
    }

    fun getPixel(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return pixels[y * width + x] and 0xFFFFFF
    }

    fun toRgbTuples(): List<Triple<Int, Int, Int>> =
        pixels.map { rgb ->
            val c = rgb and 0xFFFFFF
            Triple((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PixelFrame) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }

    companion object {
        fun solid(colorRgb: Int, width: Int = 16, height: Int = 16): PixelFrame {
            val c = colorRgb and 0xFFFFFF
            return PixelFrame(width, height, IntArray(width * height) { c })
        }

        fun clear(width: Int = 16, height: Int = 16): PixelFrame = solid(0x000000, width, height)
    }
}

/** @deprecated Prefer [PixelFrame]; kept as typealias for UI transition. */
typealias PixelBitmap = PixelFrame
