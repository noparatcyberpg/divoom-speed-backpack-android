package com.divoomspeed.backpack.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divoomspeed.backpack.renderer.PixelFrame

@Composable
fun PixelPreviewComposable(
    frame: PixelFrame,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
            .aspectRatio(1f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellW = size.width / frame.width
            val cellH = size.height / frame.height
            val gap = 1f

            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {
                    val colorInt = frame.getPixel(x, y)
                    val color = Color(colorInt)
                    drawRect(
                        color = if (colorInt == android.graphics.Color.BLACK) Color(0xFF111111) else color,
                        topLeft = Offset(x * cellW + gap, y * cellH + gap),
                        size = Size(cellW - (gap * 2), cellH - (gap * 2))
                    )
                }
            }
        }
    }
}
