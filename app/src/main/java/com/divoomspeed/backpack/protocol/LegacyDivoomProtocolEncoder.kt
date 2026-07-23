package com.divoomspeed.backpack.protocol

import com.divoomspeed.backpack.bluetooth.DeviceCapabilities
import com.divoomspeed.backpack.bluetooth.DivoomDeviceType
import com.divoomspeed.backpack.renderer.PixelFrame

class LegacyDivoomProtocolEncoder : DivoomProtocolEncoder {
    override val protocolName: String = "Legacy Divoom Pixoo Backpack RFCOMM"

    override fun supports(device: DeviceCapabilities): Boolean {
        return device.deviceType == DivoomDeviceType.CLASSIC_RFCOMM ||
                device.deviceName.contains("Pixoo", ignoreCase = true) ||
                device.deviceName.contains("Backpack", ignoreCase = true)
    }

    override fun encodeFrame(frame: PixelFrame): Result<List<ByteArray>> {
        return try {
            val rgb = frame.toRgbByteArray()
            // Command 0x44 (Static Pixel Data)
            // Payload format: [0x00, 0x0A, 0x0A, 0x04] + raw RGB data
            val header = byteArrayOf(0x00.toByte(), 0x0A.toByte(), 0x0A.toByte(), 0x04.toByte())
            val payload = ByteArray(header.size + rgb.size)
            System.arraycopy(header, 0, payload, 0, header.size)
            System.arraycopy(rgb, 0, payload, header.size, rgb.size)

            val packet = DivoomProtocolEncoder.buildPacket(0x44.toByte(), payload)
            Result.success(listOf(packet))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun encodeBrightness(brightness: Int): Result<List<ByteArray>> {
        return try {
            val clamped = brightness.coerceIn(0, 100).toByte()
            val payload = byteArrayOf(clamped)
            val packet = DivoomProtocolEncoder.buildPacket(0x74.toByte(), payload)
            Result.success(listOf(packet))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun encodeClearScreen(): Result<List<ByteArray>> {
        return try {
            val payload = byteArrayOf(0x00.toByte())
            val packet = DivoomProtocolEncoder.buildPacket(0x45.toByte(), payload)
            Result.success(listOf(packet))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
