package com.mobile.superiorutils.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.superiorutils.theme.*

// ══════════════════════════════════════════════════════════
//  Reusable Glass Card
// ══════════════════════════════════════════════════════════

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLevel1, RoundedCornerShape(24.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content
    )
}

// ══════════════════════════════════════════════════════════
//  Custom Modifiers
// ══════════════════════════════════════════════════════════

/**
 * Reusable glow effect to DRY out the nativeCanvas shadow drawing logic.
 */
fun Modifier.glow(
    color: Color,
    radius: Float = 30f,
    dy: Float = 10f,
    dx: Float = 0f,
    cornerRadius: Dp = 0.dp,
    shapeColor: Color = Color.Transparent
) = composed {
    val paint = remember(color, radius, dx, dy, shapeColor) {
        Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = shapeColor.toArgb()
                setShadowLayer(radius, dx, dy, color.toArgb())
            }
        }
    }
    
    drawBehind {
        if (cornerRadius > 0.dp) {
            drawContext.canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadius.toPx(), cornerRadius.toPx(),
                paint.asFrameworkPaint()
            )
        } else {
            // For circle bounds (e.g. mic button) where we just want the glow in the center
            drawContext.canvas.nativeCanvas.drawCircle(
                size.width / 2f, size.height / 2f, size.width / 2f,
                paint.asFrameworkPaint()
            )
        }
    }
}
