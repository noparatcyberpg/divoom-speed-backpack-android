package com.divoomspeed.backpack.renderer

data class PixelFrame(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    val totalPixels: Int
        get() = width * height

    fun getPixel(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return pixels[y * width + x]
    }

    fun toRgbByteArray(): ByteArray {
        val result = ByteArray(totalPixels * 3)
        var idx = 0
        for (color in pixels) {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            result[idx++] = r.toByte()
            result[idx++] = g.toByte()
            result[idx++] = b.toByte()
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PixelFrame
        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
