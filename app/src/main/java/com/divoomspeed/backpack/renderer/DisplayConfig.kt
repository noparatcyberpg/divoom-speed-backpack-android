package com.divoomspeed.backpack.renderer

enum class DisplayOrientation(val degrees: Int) {
    ROTATION_0(0),
    ROTATION_90(90),
    ROTATION_180(180),
    ROTATION_270(270)
}

enum class SpeedColorMode {
    SPEED_BASED,
    MONOCHROME_GREEN,
    MONOCHROME_WHITE,
    RAINBOW,
    HIGH_CONTRAST
}

data class DisplayConfig(
    val width: Int = 16,
    val height: Int = 16,
    val showUnit: Boolean = true,
    val brightness: Int = 80,
    val colorMode: SpeedColorMode = SpeedColorMode.SPEED_BASED,
    val orientation: DisplayOrientation = DisplayOrientation.ROTATION_0
)
