package com.divoomspeed.backpack.protocol

import com.divoomspeed.backpack.bluetooth.DeviceCapabilities
import com.divoomspeed.backpack.bluetooth.DivoomDeviceType
import com.divoomspeed.backpack.renderer.PixelFrame

class BackpackProtocolEncoder : DivoomProtocolEncoder {
    override val protocolName: String = "Modern Backpack M Protocol"

    override fun supports(device: DeviceCapabilities): Boolean {
        return device.deviceType == DivoomDeviceType.BLE_GATT ||
                device.deviceName.contains("Backpack M", ignoreCase = true)
    }

    override fun encodeFrame(frame: PixelFrame): Result<List<ByteArray>> {
        return try {
            val rgb = frame.toRgbByteArray()
            // Variant 1: Pixoo / Backpack M static frame header (00 0A 0A 04)
            val header1 = byteArrayOf(0x00.toByte(), 0x0A.toByte(), 0x0A.toByte(), 0x04.toByte())
            val payload1 = ByteArray(header1.size + rgb.size)
            System.arraycopy(header1, 0, payload1, 0, header1.size)
            System.arraycopy(rgb, 0, payload1, header1.size, rgb.size)
            val packet1 = DivoomProtocolEncoder.buildPacket(0x44.toByte(), payload1)

            // Variant 2: Backpack M dynamic header (00 05 01)
            val header2 = byteArrayOf(0x00.toByte(), 0x05.toByte(), 0x01.toByte())
            val payload2 = ByteArray(header2.size + rgb.size)
            System.arraycopy(header2, 0, payload2, 0, header2.size)
            System.arraycopy(rgb, 0, payload2, header2.size, rgb.size)
            val packet2 = DivoomProtocolEncoder.buildPacket(0x44.toByte(), payload2)

            Result.success(listOf(packet1, packet2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun encodeBrightness(brightness: Int): Result<List<ByteArray>> {
        val clamped = brightness.coerceIn(0, 100).toByte()
        val packet = DivoomProtocolEncoder.buildPacket(0x74.toByte(), byteArrayOf(clamped))
        return Result.success(listOf(packet))
    }

    override fun encodeClearScreen(): Result<List<ByteArray>> {
        val packet = DivoomProtocolEncoder.buildPacket(0x45.toByte(), byteArrayOf(0x00))
        return Result.success(listOf(packet))
    }
}

class CyberbagProtocolEncoder : DivoomProtocolEncoder {
    override val protocolName: String = "Cyberbag Extended Protocol"

    override fun supports(device: DeviceCapabilities): Boolean {
        return device.deviceName.contains("Cyberbag", ignoreCase = true)
    }

    override fun encodeFrame(frame: PixelFrame): Result<List<ByteArray>> {
        return try {
            val rgb = frame.toRgbByteArray()
            val header = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x10.toByte(), 0x10.toByte())
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
        val clamped = brightness.coerceIn(0, 100).toByte()
        val packet = DivoomProtocolEncoder.buildPacket(0x74.toByte(), byteArrayOf(clamped))
        return Result.success(listOf(packet))
    }

    override fun encodeClearScreen(): Result<List<ByteArray>> {
        val packet = DivoomProtocolEncoder.buildPacket(0x45.toByte(), byteArrayOf(0x00))
        return Result.success(listOf(packet))
    }
}

class UnknownProtocolEncoder : DivoomProtocolEncoder {
    override val protocolName: String = "Protocol Not Verified"

    override fun supports(device: DeviceCapabilities): Boolean = false

    override fun encodeFrame(frame: PixelFrame): Result<List<ByteArray>> {
        return Result.failure(IllegalStateException("Protocol not verified for this device. Use Bluetooth Inspector to verify GATT/SDP parameters."))
    }

    override fun encodeBrightness(brightness: Int): Result<List<ByteArray>> {
        return Result.failure(IllegalStateException("Protocol not verified for this device."))
    }

    override fun encodeClearScreen(): Result<List<ByteArray>> {
        return Result.failure(IllegalStateException("Protocol not verified for this device."))
    }
}
