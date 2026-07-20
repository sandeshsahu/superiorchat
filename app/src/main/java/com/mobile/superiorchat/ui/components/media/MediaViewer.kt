package com.mobile.superiorchat.ui.components.media

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MediaViewer(
    mediaPath: String?,
    mediaType: String?, // "photo", "video", etc.
    onDismiss: () -> Unit
) {
    val showMedia = mediaPath != null
    val currentPath = remember { mutableStateOf<String?>(null) }
    val currentType = remember { mutableStateOf<String?>(null) }

    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(showMedia, mediaPath, mediaType) {
        if (showMedia) {
            currentPath.value = mediaPath
            currentType.value = mediaType
            transitionState.targetState = true
        } else {
            transitionState.targetState = false
        }
    }

    val animatedDismiss: () -> Unit = {
        transitionState.targetState = false
    }

    LaunchedEffect(transitionState.currentState, transitionState.isIdle) {
        if (!transitionState.targetState && transitionState.isIdle) {
            onDismiss()
        }
    }

    if (!showMedia && !transitionState.currentState && transitionState.isIdle) return

    Dialog(
        onDismissRequest = { animatedDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
        ) {
            val path = currentPath.value
            val type = currentType.value
            if (path != null) {
                MediaViewerContent(
                    path = path,
                    type = type ?: "photo",
                    onDismiss = animatedDismiss
                )
            }
        }
    }
}

@Composable
private fun MediaViewerContent(
    path: String,
    type: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun userInteracted() {
        isControlsVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Auto-hide controls loop
    LaunchedEffect(isControlsVisible, lastInteractionTime) {
        if (isControlsVisible) {
            delay(3500)
            isControlsVisible = false
        }
    }

    // Drag to dismiss states
    val dragOffsetY = remember { Animatable(0f) }
    val dragOffsetX = remember { Animatable(0f) }
    val scaleFactor = remember { derivedStateOf { (1f - (Math.abs(dragOffsetY.value) / 1000f)).coerceIn(0.7f, 1f) } }
    val backdropAlpha = remember { derivedStateOf { (1f - (Math.abs(dragOffsetY.value) / 800f)).coerceIn(0f, 0.95f) } }

    // Pinch-to-zoom states
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffsetY.value
                    translationX = dragOffsetX.value
                    scaleX = scaleFactor.value
                    scaleY = scaleFactor.value
                },
            contentAlignment = Alignment.Center
        ) {
            if (type == "video") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, _, _ ->
                                scope.launch {
                                    dragOffsetY.snapTo(dragOffsetY.value + pan.y)
                                    dragOffsetX.snapTo(dragOffsetX.value + pan.x)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    var allUp = false
                                    do {
                                        val event = awaitPointerEvent()
                                        allUp = event.changes.all { !it.pressed }
                                    } while (!allUp)
                                    
                                    if (Math.abs(dragOffsetY.value) > 150f) {
                                        scope.launch {
                                            val targetY = if (dragOffsetY.value > 0) 1500f else -1500f
                                            dragOffsetY.animateTo(targetY, tween(200))
                                            onDismiss()
                                        }
                                    } else {
                                        scope.launch {
                                            launch { dragOffsetY.animateTo(0f, spring()) }
                                            launch { dragOffsetX.animateTo(0f, spring()) }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    VideoPlayerComponent(
                        path = path,
                        isControlsVisible = isControlsVisible,
                        onToggleControls = { isControlsVisible = !isControlsVisible },
                        onUserInteraction = { userInteracted() }
                    )
                }
            } else {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            when {
                                path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://") -> android.net.Uri.parse(path)
                                else -> File(path)
                            }
                        )
                        .size(2048)
                        .apply {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                bitmapConfig(Bitmap.Config.HARDWARE)
                            }
                        }
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full Screen Photo",
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryLight)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                    val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                    scope.launch {
                                        dragOffsetY.snapTo(dragOffsetY.value + pan.y)
                                        dragOffsetX.snapTo(dragOffsetX.value + pan.x)
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    var allUp = false
                                    do {
                                        val event = awaitPointerEvent()
                                        allUp = event.changes.all { !it.pressed }
                                    } while (!allUp)
                                    
                                    if (scale <= 1f) {
                                        if (Math.abs(dragOffsetY.value) > 150f) {
                                            scope.launch {
                                                val targetY = if (dragOffsetY.value > 0) 1500f else -1500f
                                                dragOffsetY.animateTo(targetY, tween(200))
                                                onDismiss()
                                            }
                                        } else {
                                            scope.launch {
                                                launch { dragOffsetY.animateTo(0f, spring()) }
                                                launch { dragOffsetX.animateTo(0f, spring()) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        // Zoom into the tap location
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val targetX = (centerX - tapOffset.x) * 1.5f
                                        val targetY = (centerY - tapOffset.y) * 1.5f
                                        offset = Offset(targetX, targetY)
                                    }
                                },
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Beautiful glassmorphic top header bar containing back/close button
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close full screen media",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerComponent(
    path: String,
    isControlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onUserInteraction: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPos by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoRatio by remember { mutableStateOf(16f / 9f) }

    // Media Player Lifecycle
    DisposableEffect(path) {
        val mp = MediaPlayer().apply {
            try {
                if (path.startsWith("content://")) {
                    setDataSource(context, android.net.Uri.parse(path))
                } else {
                    setDataSource(path)
                }
                setOnPreparedListener {
                    isPrepared = true
                    duration = it.duration
                    it.start()
                    isPlaying = true
                }
                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        videoRatio = width.toFloat() / height.toFloat()
                    }
                }
                setOnCompletionListener {
                    isPlaying = false
                    seekTo(0)
                }
                prepareAsync()
            } catch (e: Exception) {
                // handle error
            }
        }
        mediaPlayer = mp

        onDispose {
            try {
                if (mp.isPlaying) mp.stop()
            } catch (e: Exception) {}
            mp.release()
            mediaPlayer = null
        }
    }

    // Keep track of playback position
    LaunchedEffect(isPlaying, isPrepared) {
        if (isPlaying && isPrepared) {
            while (true) {
                mediaPlayer?.let {
                    currentPos = it.currentPosition
                }
                delay(250)
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(videoRatio, matchHeightConstraintsFirst = videoRatio < 1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggleControls()
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            mediaPlayer?.setSurface(android.view.Surface(surface))
                        }
                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            mediaPlayer?.setSurface(null)
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isPrepared) {
            CircularProgressIndicator(color = PrimaryLight)
        }

        // Custom video controls overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Seek bar and text indicators at the bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    onUserInteraction()
                                    mediaPlayer?.let {
                                        if (it.isPlaying) {
                                            it.pause()
                                            isPlaying = false
                                        } else {
                                            it.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isPlaying) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp).padding(2.dp)) {
                                        val barWidth = size.width / 3.5f
                                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(barWidth, size.height))
                                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width - barWidth, 0f), size = androidx.compose.ui.geometry.Size(barWidth, size.height))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatTime(currentPos),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = if (duration > 0) currentPos.toFloat() / duration else 0f,
                        onValueChange = { percent ->
                            onUserInteraction()
                            mediaPlayer?.let {
                                val targetMs = (percent * duration).toInt()
                                it.seekTo(targetMs)
                                currentPos = targetMs
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
