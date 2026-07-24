package com.mobile.superiorchat.ui.components

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.superiorchat.theme.*

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

/**
 * Applies a subtle scaling bounce animation when the user taps on the composable.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit = {}
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        label = "bounceClick_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(isPressed) {
            awaitPointerEventScope {
                isPressed = if (isPressed) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(requireUnconsumed = false)
                    true
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onClick() }
        )
}

// ══════════════════════════════════════════════════════════
//  Lumina Aesthetic (DRY Components)
// ══════════════════════════════════════════════════════════

/**
 * Standard button colors for the "Lumina" active state (PrimaryLight background, deep blue icon/text)
 */
@Composable
fun luminaButtonColors() = androidx.compose.material3.ButtonDefaults.buttonColors(
    containerColor = PrimaryLight,
    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
)

/**
 * Standard switch colors for the "Lumina" active state.
 */
@Composable
fun luminaSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
    checkedTrackColor = PrimaryLight,
    uncheckedThumbColor = TextSecondary,
    uncheckedTrackColor = SurfaceLevel2,
    uncheckedBorderColor = DividerColor
)

/**
 * Standard navigation drawer item colors for the "Lumina" selected state.
 */
@Composable
fun luminaNavigationDrawerItemColors() = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
    selectedContainerColor = PrimaryLight,
    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
    unselectedContainerColor = Color.Transparent,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)


