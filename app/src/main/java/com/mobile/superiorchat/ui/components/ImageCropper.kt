package com.mobile.superiorchat.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.theme.DividerColor
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun ImageCropper(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirm: (Uri, Float, Float, Float) -> Unit
) {
    val context = LocalContext.current
    var imageRatio by remember { mutableStateOf(1f) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use { 
                    BitmapFactory.decodeStream(it, null, options) 
                }
                if (options.outWidth > 0 && options.outHeight > 0) {
                    imageRatio = options.outWidth.toFloat() / options.outHeight.toFloat()
                }
            } catch (e: Exception) {
                // Ignore, defaults to 1f
            }
        }
    }

    // Crop box state — normalized fractions [0..1] relative to canvas
    // cropX, cropY = top-left corner; cropSize = side length relative to minimum dimension
    var cropX by remember { mutableStateOf(0.14f) }
    var cropY by remember { mutableStateOf(0.14f) }
    var cropSize by remember { mutableStateOf(0.72f) }

    // Animated crop border pulse
    val borderAlpha by rememberInfiniteTransition(label = "crop_pulse").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
        ) {
            Column {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Crop Photo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { onCropConfirm(imageUri, cropX, cropY, cropSize) }) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Use", color = PrimaryLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Crop canvas wrapper to prevent overflow for tall images
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    // Crop canvas — BoxWithConstraints to get pixel size for hit tests
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(imageRatio, matchHeightConstraintsFirst = imageRatio < 1f)
                            .background(Color.Black)
                    ) {
                        val canvasWidthPx = constraints.maxWidth.toFloat()
                        val canvasHeightPx = constraints.maxHeight.toFloat()
                        val minDimPx = Math.min(canvasWidthPx, canvasHeightPx)

                        // Background image (static)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Crop overlay — scrim + box + grid + handles (drawn on Canvas)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                // Move crop box by dragging INSIDE it
                                .pointerInput(canvasWidthPx, canvasHeightPx, minDimPx) {
                                    detectDragGestures { change, dragAmount ->
                                        val bx = cropX * canvasWidthPx
                                        val by = cropY * canvasHeightPx
                                        val bs = cropSize * minDimPx
                                        val touchX = change.position.x
                                        val touchY = change.position.y
                                        // Only move if touch is INSIDE crop box (not near corners)
                                        val handleZone = 36f
                                        val insideX = touchX in (bx + handleZone)..(bx + bs - handleZone)
                                        val insideY = touchY in (by + handleZone)..(by + bs - handleZone)
                                        if (insideX && insideY) {
                                            val newX = cropX + dragAmount.x / canvasWidthPx
                                            val newY = cropY + dragAmount.y / canvasHeightPx
                                            val maxCropX = (canvasWidthPx - bs) / canvasWidthPx
                                            val maxCropY = (canvasHeightPx - bs) / canvasHeightPx
                                            cropX = newX.coerceIn(0f, maxCropX)
                                            cropY = newY.coerceIn(0f, maxCropY)
                                        }
                                    }
                                }
                        ) {
                            val bx = cropX * size.width
                            val by = cropY * size.height
                            val bs = cropSize * minDimPx

                            // Dark scrim (4 rects around crop box)
                            val scrim = Color.Black.copy(alpha = 0.65f)
                            drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, by))
                            drawRect(scrim, topLeft = Offset(0f, by + bs), size = Size(size.width, size.height - by - bs))
                            drawRect(scrim, topLeft = Offset(0f, by), size = Size(bx, bs))
                            drawRect(scrim, topLeft = Offset(bx + bs, by), size = Size(size.width - bx - bs, bs))

                            // Crop border
                            drawRect(
                                color = Primary.copy(alpha = borderAlpha),
                                topLeft = Offset(bx, by),
                                size = Size(bs, bs),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Rule-of-thirds grid
                            val third = bs / 3f
                            val gridColor = Color.White.copy(alpha = 0.18f)
                            for (i in 1..2) {
                                drawLine(gridColor, Offset(bx + third * i, by), Offset(bx + third * i, by + bs), 1.dp.toPx())
                                drawLine(gridColor, Offset(bx, by + third * i), Offset(bx + bs, by + third * i), 1.dp.toPx())
                            }

                            // Corner L-handles
                            val hLen = 20.dp.toPx()
                            val hW = 3.dp.toPx()
                            val hColor = PrimaryLight
                            // TL
                            drawLine(hColor, Offset(bx, by), Offset(bx + hLen, by), hW)
                            drawLine(hColor, Offset(bx, by), Offset(bx, by + hLen), hW)
                            // TR
                            drawLine(hColor, Offset(bx + bs, by), Offset(bx + bs - hLen, by), hW)
                            drawLine(hColor, Offset(bx + bs, by), Offset(bx + bs, by + hLen), hW)
                            // BL
                            drawLine(hColor, Offset(bx, by + bs), Offset(bx + hLen, by + bs), hW)
                            drawLine(hColor, Offset(bx, by + bs), Offset(bx, by + bs - hLen), hW)
                            // BR
                            drawLine(hColor, Offset(bx + bs, by + bs), Offset(bx + bs - hLen, by + bs), hW)
                            drawLine(hColor, Offset(bx + bs, by + bs), Offset(bx + bs, by + bs - hLen), hW)
                        }

                        // ── Corner resize handles (transparent hit areas on top of Canvas) ──
                        val handleSizeDp: Dp = 44.dp
                        
                        val updateCrop: (Float, Float, Boolean, Boolean) -> Unit = { dragX, dragY, isLeft, isTop ->
                            val currentXPx = cropX * canvasWidthPx
                            val currentYPx = cropY * canvasHeightPx
                            val currentSizePx = cropSize * minDimPx
                            
                            val sizeDeltaX = if (isLeft) -dragX else dragX
                            val sizeDeltaY = if (isTop) -dragY else dragY
                            val sizeDelta = (sizeDeltaX + sizeDeltaY) / 2f
                            
                            var newSizePx = currentSizePx + sizeDelta
                            if (newSizePx < minDimPx * 0.20f) newSizePx = minDimPx * 0.20f
                            
                            val maxWPx = if (isLeft) currentXPx + currentSizePx else canvasWidthPx - currentXPx
                            val maxHPx = if (isTop) currentYPx + currentSizePx else canvasHeightPx - currentYPx
                            val maxSizePx = kotlin.math.min(maxWPx, maxHPx)
                            if (newSizePx > maxSizePx) newSizePx = maxSizePx
                            
                            val newXPx = if (isLeft) currentXPx + currentSizePx - newSizePx else currentXPx
                            val newYPx = if (isTop) currentYPx + currentSizePx - newSizePx else currentYPx
                            
                            cropX = newXPx / canvasWidthPx
                            cropY = newYPx / canvasHeightPx
                            cropSize = newSizePx / minDimPx
                        }

                        // TOP-LEFT handle
                        Box(
                            modifier = Modifier
                                .size(handleSizeDp)
                                .offset(
                                    x = (cropX * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                    y = (cropY * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                                )
                                .pointerInput(canvasWidthPx, canvasHeightPx) {
                                    detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, true, true) }
                                }
                        )

                        // BOTTOM-RIGHT handle
                        Box(
                            modifier = Modifier
                                .size(handleSizeDp)
                                .offset(
                                    x = (cropX * canvasWidthPx + cropSize * minDimPx).pxToDp(density) - handleSizeDp/2,
                                    y = (cropY * canvasHeightPx + cropSize * minDimPx).pxToDp(density) - handleSizeDp/2
                                )
                                .pointerInput(canvasWidthPx, canvasHeightPx) {
                                    detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, false, false) }
                                }
                        )

                        // TOP-RIGHT handle
                        Box(
                            modifier = Modifier
                                .size(handleSizeDp)
                                .offset(
                                    x = (cropX * canvasWidthPx + cropSize * minDimPx).pxToDp(density) - handleSizeDp/2,
                                    y = (cropY * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                                )
                                .pointerInput(canvasWidthPx, canvasHeightPx) {
                                    detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, false, true) }
                                }
                        )

                        // BOTTOM-LEFT handle
                        Box(
                            modifier = Modifier
                                .size(handleSizeDp)
                                .offset(
                                    x = (cropX * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                    y = (cropY * canvasHeightPx + cropSize * minDimPx).pxToDp(density) - handleSizeDp/2
                                )
                                .pointerInput(canvasWidthPx, canvasHeightPx) {
                                    detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, true, false) }
                                }
                        )
                    } // End of BoxWithConstraints
                } // End of wrapper Box

                // Hint row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.OpenWith, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Drag inside to move · drag corners to resize", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ── px/dp conversion helpers used in BoxWithConstraints ──
private val density: Float
    @Composable get() = LocalDensity.current.density

private fun Float.pxToDp(density: Float) = (this / density).dp
private fun Dp.toPx(density: Float) = (this.value * density)
