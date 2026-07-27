package com.divoom.navigationarrow.domain.protocol

import com.divoom.navigationarrow.domain.model.PixelFrame
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

/**
 * Port of divoom_protocol.encoder.encode_image
 * commit f9b661f8e06a6d40be91c800fed656c7402bd2ac
 *
 * Wire layout (inner payload, before envelope):
 * 44 00 0a 0a 04 aa [fs_lo fs_hi] F4 01 00 [color_count] [palette RGB...] [LSB-first indices]
 */
object StaticImageEncoder {

    fun encodeImage(frame: PixelFrame): ByteArray {
        require(frame.width == 16 && frame.height == 16) {
            "expected 16x16 frame, got ${frame.width}x${frame.height}"
        }
        require(frame.pixels.size == 256) {
            "expected 256 pixels, got ${frame.pixels.size}"
        }
        return encodeRgbList(frame.toRgbTuples())
    }

    fun encodeRgbList(pixels: List<Triple<Int, Int, Int>>): ByteArray {
        if (pixels.size != 256) {
            throw IllegalArgumentException("expected 256 pixels (16x16), got ${pixels.size}")
        }

        val palette = mutableListOf<Triple<Int, Int, Int>>()
        val indexMap = LinkedHashMap<Int, Int>()
        for ((r, g, b) in pixels) {
            val key = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
            if (!indexMap.containsKey(key)) {
                indexMap[key] = palette.size
                palette.add(Triple(r and 0xFF, g and 0xFF, b and 0xFF))
            }
        }

        val paletteSize = palette.size
        val bitsPerPixel = if (paletteSize > 1) {
            max(1, ceil(log2(paletteSize.toDouble())).toInt())
        } else {
            1
        }

        // Pack LSB-first, no bit-reversal (matches Python encoder bit-by-bit loop)
        val pixelBytes = ArrayList<Int>()
        var currentByte = 0
        var bitPos = 0
        for ((r, g, b) in pixels) {
            val key = ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
            val idx = indexMap[key]!!
            for (i in 0 until bitsPerPixel) {
                if (((idx shr i) and 1) != 0) {
                    currentByte = currentByte or (1 shl bitPos)
                }
                bitPos++
                if (bitPos == 8) {
                    pixelBytes.add(currentByte and 0xFF)
                    currentByte = 0
                    bitPos = 0
                }
            }
        }
        if (bitPos > 0) {
            pixelBytes.add(currentByte and 0xFF)
        }

        val paletteBytes = ByteArray(paletteSize * 3)
        palette.forEachIndexed { i, (r, g, b) ->
            paletteBytes[i * 3] = (r and 0xFF).toByte()
            paletteBytes[i * 3 + 1] = (g and 0xFF).toByte()
            paletteBytes[i * 3 + 2] = (b and 0xFF).toByte()
        }

        val frameSize = 7 + paletteBytes.size + pixelBytes.size
        val fsLo = frameSize and 0xFF
        val fsHi = (frameSize shr 8) and 0xFF

        val out = ByteArray(6 + 2 + 3 + 1 + paletteBytes.size + pixelBytes.size)
        // Header: 44 00 0a 0a 04 aa
        out[0] = 0x44
        out[1] = 0x00
        out[2] = 0x0A
        out[3] = 0x0A
        out[4] = 0x04
        out[5] = 0xAA.toByte()
        out[6] = fsLo.toByte()
        out[7] = fsHi.toByte()
        // Mystery bytes — NOT zeros
        out[8] = 0xF4.toByte()
        out[9] = 0x01
        out[10] = 0x00
        out[11] = (paletteSize and 0xFF).toByte()
        System.arraycopy(paletteBytes, 0, out, 12, paletteBytes.size)
        var offset = 12 + paletteBytes.size
        for (b in pixelBytes) {
            out[offset++] = (b and 0xFF).toByte()
        }
        return out
    }
}
