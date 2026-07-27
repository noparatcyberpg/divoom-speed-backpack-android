package com.divoom.navigationarrow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.divoom.navigationarrow.domain.model.PixelFrame
import com.divoom.navigationarrow.ui.theme.SurfaceDark
import com.divoom.navigationarrow.ui.theme.SurfaceVariantDark

@Composable
fun PixelCanvas(
    bitmap: PixelFrame,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(2.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellWidth = size.width / bitmap.width
            val cellHeight = size.height / bitmap.height
            val gap = 1f // Grid line gap in pixels

            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val rgbInt = bitmap.getPixel(x, y)
                    val r = (rgbInt shr 16) and 0xFF
                    val g = (rgbInt shr 8) and 0xFF
                    val b = rgbInt and 0xFF
                    val color = Color(r, g, b)

                    // Draw pixel cell with subtle rounded LED matrix effect
                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellWidth + gap, y * cellHeight + gap),
                        size = Size(cellWidth - gap * 2, cellHeight - gap * 2)
                    )
                }
            }

            // Draw pixel grid lines
            for (i in 0..bitmap.width) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = Offset(i * cellWidth, 0f),
                    end = Offset(i * cellWidth, size.height),
                    strokeWidth = 1f
                )
            }
            for (j in 0..bitmap.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = Offset(0f, j * cellHeight),
                    end = Offset(size.width, j * cellHeight),
                    strokeWidth = 1f
                )
            }
        }
    }
}
