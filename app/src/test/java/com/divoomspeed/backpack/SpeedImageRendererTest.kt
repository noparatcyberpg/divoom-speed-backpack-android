package com.divoomspeed.backpack

import com.divoomspeed.backpack.renderer.DefaultSpeedImageRenderer
import com.divoomspeed.backpack.renderer.DisplayConfig
import com.divoomspeed.backpack.renderer.SpeedColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SpeedImageRendererTest {

    private val renderer = DefaultSpeedImageRenderer()

    @Test
    fun testColorModeThresholds() {
        val green = renderer.getSpeedColor(25, SpeedColorMode.SPEED_BASED)
        assertEquals(DefaultSpeedImageRenderer.COLOR_GREEN, green)

        val yellow = renderer.getSpeedColor(50, SpeedColorMode.SPEED_BASED)
        assertEquals(DefaultSpeedImageRenderer.COLOR_YELLOW, yellow)

        val orange = renderer.getSpeedColor(75, SpeedColorMode.SPEED_BASED)
        assertEquals(DefaultSpeedImageRenderer.COLOR_ORANGE, orange)

        val red = renderer.getSpeedColor(110, SpeedColorMode.SPEED_BASED)
        assertEquals(DefaultSpeedImageRenderer.COLOR_RED, red)
    }

    @Test
    fun testRenderFrameDimensions() {
        val config16 = DisplayConfig(width = 16, height = 16)
        val frame16 = renderer.render(50, config16)
        assertEquals(16, frame16.width)
        assertEquals(16, frame16.height)
        assertEquals(256, frame16.pixels.size)
        assertEquals(768, frame16.toRgbByteArray().size)

        val config64 = DisplayConfig(width = 64, height = 64)
        val frame64 = renderer.render(50, config64)
        assertEquals(64, frame64.width)
        assertEquals(64, frame64.height)
        assertEquals(4096, frame64.pixels.size)
    }
}
