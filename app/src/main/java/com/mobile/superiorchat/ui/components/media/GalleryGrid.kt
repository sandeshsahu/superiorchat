package com.mobile.superiorchat.ui.components.media

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.painter.ColorPainter
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.Secondary
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.ui.skeletonEffect
import com.mobile.superiorchat.ui.SkeletonGalleryItem
import com.mobile.superiorchat.ui.SkeletonContainer
import kotlinx.coroutines.delay

import com.mobile.superiorchat.data.repository.LocalMediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryGrid(
    preLoadedMedia: List<LocalMediaItem>? = null,
    maxSelection: Int = Int.MAX_VALUE,
    showVideos: Boolean = true,
    onDismiss: () -> Unit,
    onMediaSelected: (List<LocalMediaItem>) -> Boolean,
    onCameraClick: () -> Unit
) {
    val context = LocalContext.current
    val selectedItems = remember { mutableStateListOf<LocalMediaItem>() }
    var previewMedia by remember { mutableStateOf<LocalMediaItem?>(null) }
    var isLoading by remember { mutableStateOf(preLoadedMedia == null) }
    var localMediaState by remember { mutableStateOf<List<LocalMediaItem>>(preLoadedMedia ?: emptyList()) }
    
    LaunchedEffect(preLoadedMedia) {
        if (preLoadedMedia == null) {
            isLoading = true
            // Delay to allow MediaPicker's entry animation (300ms) to complete before stealing CPU for DB/Cursor query
            delay(350)
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val media = com.mobile.superiorchat.core.AppGraph.appRepository.getAllLocalMedia(context)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    localMediaState = media
                    isLoading = false
                }
            }
        } else {
            localMediaState = preLoadedMedia
            isLoading = false
        }
    }

    val folders = remember(localMediaState) {
        listOf("All Folders") + localMediaState.map { it.bucketName }.distinct().sorted()
    }
    var selectedFolder by remember { mutableStateOf("All Folders") }
    
    val mediaTypes = listOf("All Media", "Images", "Videos")
    var selectedMediaType by remember { mutableStateOf("All Media") }

    val filteredMedia = remember(localMediaState, selectedFolder, selectedMediaType, showVideos) {
        var result = if (showVideos) localMediaState else localMediaState.filter { !it.isVideo }
        
        result = when (selectedMediaType) {
            "Images" -> result.filter { !it.isVideo }
            "Videos" -> result.filter { it.isVideo }
            else -> result
        }
        
        if (selectedFolder != "All Folders") {
            result = result.filter { it.bucketName == selectedFolder }
        }
        
        result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterDropdown(
                            selectedText = selectedMediaType,
                            options = mediaTypes,
                            onOptionSelected = { selectedMediaType = it }
                        )
                        FilterDropdown(
                            selectedText = selectedFolder,
                            options = folders,
                            onOptionSelected = { selectedFolder = it },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        AnimatedContent(
                            targetState = selectedItems.size,
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "fab_counter"
                        ) { count ->
                            Text("Send ($count)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black) },
                    onClick = {
                        val success = onMediaSelected(selectedItems.toList())
                        if (success) {
                            onDismiss()
                        }
                    },
                    containerColor = PrimaryLight,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        },
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(18) {
                        SkeletonContainer(
                            modifier = Modifier.aspectRatio(1f),
                            cornerRadius = 12.dp
                        )
                    }
                }
            } else {
                if (filteredMedia.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No media found in this album",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Camera tile
                        item {
                            CameraGridTile(onCameraClick = onCameraClick)
                        }

                        // Grid items
                        items(filteredMedia, key = { it.id }) { media ->
                            val isSelected = selectedItems.any { it.uri == media.uri }
                            MediaGridTile(
                                uri = media.uri,
                                isVideo = media.isVideo,
                                duration = media.duration,
                                isSelected = isSelected,
                                showCheckbox = maxSelection > 1,
                                onClick = {
                                    if (isSelected) {
                                        selectedItems.removeAll { it.uri == media.uri }
                                    } else {
                                        if (maxSelection == 1) {
                                            selectedItems.clear()
                                            selectedItems.add(media)
                                            val success = onMediaSelected(listOf(media))
                                            if (success) {
                                                onDismiss()
                                            }
                                        } else if (selectedItems.size < maxSelection) {
                                            selectedItems.add(media)
                                        }
                                    }
                                },
                                onLongPressStart = {
                                    previewMedia = media
                                },
                                onLongPressEnd = {
                                    previewMedia = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Media Preview Overlay (Long-Press)
    MediaViewer(
        mediaPath = previewMedia?.uri?.toString(),
        mediaType = if (previewMedia?.isVideo == true) "video" else "photo",
        onDismiss = { previewMedia = null }
    )
}

@Composable
private fun CameraGridTile(onCameraClick: () -> Unit) {
    val cameraPulse = rememberInfiniteTransition(label = "camera_pulse")
    val pulseScale by cameraPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera_pulse_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.15f),
                        Secondary.copy(alpha = 0.15f)
                    )
                )
            )
            .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onCameraClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Open Camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Camera",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridTile(
    uri: Uri,
    isVideo: Boolean,
    duration: String?,
    isSelected: Boolean,
    showCheckbox: Boolean,
    onClick: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit
) {
    val context = LocalContext.current
    val imageScale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "image_select_scale"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.4f else 0f,
        animationSpec = tween(200),
        label = "overlay_alpha"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "check_scale"
    )

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLevel2)
            .pointerInput(uri) {
                detectTapGestures(
                    onPress = {
                        val job = CoroutineScope(Dispatchers.Main).launch {
                            delay(150)
                            currentOnLongPressStart()
                        }
                        val success = tryAwaitRelease()
                        job.cancel()
                        currentOnLongPressEnd()
                        if (success && !job.isCompleted) {
                            currentOnClick()
                        }
                    }
                )
            }
            .scale(imageScale)
    ) {
        SkeletonGalleryItem()
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .size(300) // Downsample grid thumbnails
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .videoFrameMillis(1000)
                .crossfade(true)
                .build(),
            contentDescription = "Media thumbnail",
            error = ColorPainter(SurfaceLevel2),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dimming overlay on selection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )

        if (showCheckbox) {
            // Selection checkbox badge with spring scale physics
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) PrimaryLight else Color.Black.copy(alpha = 0.4f)
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 400f)),
                    exit = scaleOut(tween(200))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(checkScale)
                    )
                }
            }
        }

        // Video Duration Overlay (with linear gradient scrim for contrast)
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = duration ?: "0:00",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



@Composable
private fun FilterDropdown(
    selectedText: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select option",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceLevel2)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(option, color = MaterialTheme.colorScheme.onSurface)
                            if (option == selectedText) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
