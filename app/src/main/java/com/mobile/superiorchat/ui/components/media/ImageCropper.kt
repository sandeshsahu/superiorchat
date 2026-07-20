package com.mobile.superiorchat.ui.components.media

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.Secondary
import com.mobile.superiorchat.ui.components.bounceClick

@Composable
fun ImageCropper(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirm: (Uri, Float, Float, Float) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    var imageSize by remember { mutableStateOf(Size.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            // Fixed crop box size (e.g. 85% of the shortest screen dimension)
            val cropBoxSizePx = minOf(screenWidthPx, screenHeightPx) * 0.85f

            var baseW = 0f
            var baseH = 0f
            var baseScale = 0f

            if (imageSize != Size.Zero) {
                // Calculate how much to scale the image initially so it fills the crop box perfectly
                baseScale = maxOf(
                    cropBoxSizePx / imageSize.width,
                    cropBoxSizePx / imageSize.height
                )
                baseW = imageSize.width * baseScale
                baseH = imageSize.height * baseScale
            }

            // The Pan & Zoom touch receiver over the whole screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageSize) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (imageSize == Size.Zero) return@detectTransformGestures
                            
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            
                            // Math to zoom in based on where the fingers are (centroid)
                            val screenCenterX = screenWidthPx / 2f
                            val screenCenterY = screenHeightPx / 2f
                            val centroidX = centroid.x - screenCenterX
                            val centroidY = centroid.y - screenCenterY
                            
                            val ratio = newScale / oldScale
                            val newOffsetX = (offset.x - centroidX) * ratio + centroidX + pan.x
                            val newOffsetY = (offset.y - centroidY) * ratio + centroidY + pan.y
                            
                            // Bounds checking so you can't drag the image edge inside the crop box
                            val wRend = baseW * newScale
                            val hRend = baseH * newScale
                            val maxX = (wRend - cropBoxSizePx) / 2f
                            val maxY = (hRend - cropBoxSizePx) / 2f
                            
                            scale = newScale
                            offset = Offset(
                                x = newOffsetX.coerceIn(-maxX, maxX),
                                y = newOffsetY.coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // ── Image Layer ──
                if (imageSize != Size.Zero) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .size(coil.size.Size.ORIGINAL) // Keep high-res for zooming
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(
                                width = (baseW / density).dp,
                                height = (baseH / density).dp
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                } else {
                    // Invisible image just to fetch intrinsic dimensions from Coil
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .size(coil.size.Size.ORIGINAL)
                            .build(),
                        contentDescription = null,
                        onSuccess = { state ->
                            val w = state.result.drawable.intrinsicWidth.toFloat()
                            val h = state.result.drawable.intrinsicHeight.toFloat()
                            if (w > 0 && h > 0) {
                                imageSize = Size(w, h)
                            }
                        },
                        modifier = Modifier.alpha(0f)
                    )
                }

                // ── Scrim and Crop Box Layer ──
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val halfBox = cropBoxSizePx / 2f
                    
                    val left = cx - halfBox
                    val top = cy - halfBox
                    
                    val scrimColor = Color.Black.copy(alpha = 0.70f)
                    
                    // Draw 4 dark rectangles around the transparent crop box hole
                    drawRect(scrimColor, topLeft = Offset.Zero, size = Size(size.width, top))
                    drawRect(scrimColor, topLeft = Offset(0f, top + cropBoxSizePx), size = Size(size.width, size.height - top - cropBoxSizePx))
                    drawRect(scrimColor, topLeft = Offset(0f, top), size = Size(left, cropBoxSizePx))
                    drawRect(scrimColor, topLeft = Offset(left + cropBoxSizePx, top), size = Size(size.width - left - cropBoxSizePx, cropBoxSizePx))
                    
                    // Draw bright border around the crop box
                    drawRect(
                        color = Color.White.copy(alpha = 0.95f),
                        topLeft = Offset(left, top),
                        size = Size(cropBoxSizePx, cropBoxSizePx),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Subtle rule of thirds grid inside
                    val third = cropBoxSizePx / 3f
                    val gridColor = Color.White.copy(alpha = 0.25f)
                    for (i in 1..2) {
                        drawLine(gridColor, Offset(left + third * i, top), Offset(left + third * i, top + cropBoxSizePx), 1.dp.toPx())
                        drawLine(gridColor, Offset(left, top + third * i), Offset(left + cropBoxSizePx, top + third * i), 1.dp.toPx())
                    }
                }
            }

            // ── Top Action Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .bounceClick(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Instruction Title
                Text(
                    "Move and Scale",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.40f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )

                // Use/Confirm Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Primary, Secondary)))
                        .bounceClick(onClick = {
                            if (imageSize == Size.Zero) return@bounceClick
                            
                            // Map the current visual pan/zoom back to crop fractions (0.0 - 1.0) for the backend
                            val wRend = baseW * scale
                            val hRend = baseH * scale
                            
                            val xRend = (wRend - cropBoxSizePx) / 2f - offset.x
                            val yRend = (hRend - cropBoxSizePx) / 2f - offset.y
                            
                            val cropXFrac = xRend / wRend
                            val cropYFrac = yRend / hRend
                            
                            val minDim = minOf(imageSize.width, imageSize.height)
                            val pixelCropSize = cropBoxSizePx / (baseScale * scale)
                            val cropSizeFrac = pixelCropSize / minDim
                            
                            onCropConfirm(imageUri, cropXFrac, cropYFrac, cropSizeFrac)
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Use", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
