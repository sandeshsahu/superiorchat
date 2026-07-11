package com.mobile.superiorutils.ui.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorutils.media.AudioPlayer
import com.mobile.superiorutils.data.entity.MessageStatus
import java.util.Locale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass

fun getAudioDuration(path: String): Long {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        time?.toLong() ?: 0L
    } catch (e: Exception) {
        0L
    }
}

// ═══════════════════════════════════════════════════════════
//  WaveformSeekBar — Scrolling right-to-left when playing
// ═══════════════════════════════════════════════════════════

@Composable
fun WaveformSeekBar(
    progress: Float,
    durationMs: Long,
    onProgressChange: ((Float) -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier
) {
    // Exact bar heights pattern from Stitch Voice Note design
    val stitchHeights = remember {
        listOf(
            2f/6f, 4f/6f, 3f/6f, 5f/6f, 2f/6f, 4f/6f, 3f/6f,
            6f/6f, 4f/6f, 2f/6f, 5f/6f, 3f/6f, 4f/6f, 2f/6f
        )
    }

    // Generate total bars for scroll window
    val totalBars = 80
    val visibleBars = 14
    
    val barHeights = remember {
        List(totalBars) { index ->
            stitchHeights[index % stitchHeights.size]
        }
    }

    val density = LocalContext.current.resources.displayMetrics.density

        val barWidthPx = 3f * density
        val gapPx = 2f * density
        val totalBarWidthPx = barWidthPx + gapPx

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(onDrag) {
                if (onDrag == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = down.changes.firstOrNull() ?: continue

                        if (downChange.pressed) {
                            downChange.consume()
                            while (true) {
                                val moveEvent = awaitPointerEvent(PointerEventPass.Initial)
                                val moveChange = moveEvent.changes.firstOrNull() ?: break
                                moveChange.consume()

                                if (!moveChange.pressed) break

                                val dragAmount = moveChange.position.x - moveChange.previousPosition.x
                                onDrag(dragAmount)
                            }
                        }
                    }
                }
            }
    ) {
        val totalScrollWidth = (totalBars - visibleBars) * totalBarWidthPx

        // Calculate scroll offset based on playback progress
        val scrollOffset = progress * (totalBars - visibleBars)
        val startIndex = scrollOffset.toInt()
        val fraction = scrollOffset - startIndex

        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.9f
        val activeIndex = progress * totalBars

        // Draw visible bars + 1 extra for smooth sliding edge transitions
        for (i in 0..visibleBars) {
            val barIndex = startIndex + i
            if (barIndex >= totalBars) break

            val heightFactor = barHeights[barIndex]
            val barHeight = maxBarHeight * heightFactor

            // Shift bars left by the fractional offset
            val x = (i - fraction) * totalBarWidthPx

            // Determine if this bar is in the played portion
            val isPlayed = progress > 0f && (barIndex.toFloat() / totalBars) <= progress
            val color = if (isPlayed) playedColor else unplayedColor

            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidthPx, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * density)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  AudioBubble — Main Entry
// ═══════════════════════════════════════════════════════════

@Composable
fun AudioBubble(
    mediaLocalPath: String?,
    mediaUrl: String?,
    mediaType: String,
    status: MessageStatus,
    isFromMe: Boolean,
    onDownloadClick: () -> Unit
) {
    val currentPath by AudioPlayer.currentPlayingPath.collectAsState()
    val isPlayingFlow by AudioPlayer.isPlaying.collectAsState()

    val isDownloaded = mediaLocalPath != null
    val isCurrent = isDownloaded && currentPath == mediaLocalPath
    val isPlaying = isCurrent && isPlayingFlow

    var localDurationMs by remember(mediaLocalPath) { mutableStateOf(0L) }
    LaunchedEffect(mediaLocalPath) {
        if (mediaLocalPath != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                localDurationMs = getAudioDuration(mediaLocalPath)
            }
        }
    }

    val playedColor = if (isFromMe) Color.White else Color(0xFFC0C1FF)
    val unplayedColor = if (isFromMe) Color.White.copy(alpha = 0.4f) else Color(0xFFC0C1FF).copy(alpha = 0.3f)
    val textDurationColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
    val textDurationWeight = if (isFromMe) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal

    val buttonBg = if (isFromMe) Color.White else Color(0xFF353535)
    val buttonIconTint = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFC0C1FF)
    val isDownloading = status == MessageStatus.SENDING
    val isFailed = status == MessageStatus.FAILED

    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            ActiveAudioPlayer(
                mediaLocalPath = mediaLocalPath,
                isPlaying = isPlaying,
                isDownloading = isDownloading,
                isFailed = isFailed,
                isDownloaded = isDownloaded,
                onDownloadClick = onDownloadClick,
                playedColor = playedColor,
                unplayedColor = unplayedColor,
                textDurationColor = textDurationColor,
                textDurationWeight = textDurationWeight,
                buttonBg = buttonBg,
                buttonIconTint = buttonIconTint
            )
        } else {
            InactiveAudioPlayer(
                mediaLocalPath = mediaLocalPath,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                isDownloading = isDownloading,
                isFailed = isFailed,
                isDownloaded = isDownloaded,
                onDownloadClick = onDownloadClick,
                playedColor = playedColor,
                unplayedColor = unplayedColor,
                textDurationColor = textDurationColor,
                textDurationWeight = textDurationWeight,
                buttonBg = buttonBg,
                buttonIconTint = buttonIconTint,
                localDurationMs = localDurationMs
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  ActiveAudioPlayer — currently playing
// ═══════════════════════════════════════════════════════════

@Composable
fun ActiveAudioPlayer(
    mediaLocalPath: String,
    isPlaying: Boolean,
    isDownloading: Boolean,
    isFailed: Boolean,
    isDownloaded: Boolean,
    onDownloadClick: () -> Unit,
    playedColor: Color,
    unplayedColor: Color,
    textDurationColor: Color,
    textDurationWeight: androidx.compose.ui.text.font.FontWeight,
    buttonBg: Color,
    buttonIconTint: Color
) {
    val context = LocalContext.current
    val progressFlow by AudioPlayer.progress.collectAsState()
    val durationFlow by AudioPlayer.durationMs.collectAsState()
    val positionFlow by AudioPlayer.currentPositionMs.collectAsState()
    val isCompletedFlow by AudioPlayer.isCompleted.collectAsState()

    val totalSec = if (durationFlow > 0) durationFlow / 1000 else 0
    val currentSec = positionFlow / 1000
    val isCompleted = isCompletedFlow

    // Play/Pause Button (40dp diameter to match Stitch w-10)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable {
                AudioPlayer.play(context, mediaLocalPath)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = buttonIconTint,
                modifier = Modifier.size(20.dp)
            )
        } else if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Replay",
                tint = buttonIconTint,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = buttonIconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.width(12.dp))

    val density = LocalContext.current.resources.displayMetrics.density
    val totalBarWidthPx = (3f * density) + (2f * density)
    val totalBars = 80
    val visibleBars = 14
    val totalScrollWidth = (totalBars - visibleBars) * totalBarWidthPx

    // Waveform (Exactly 14 bars, 70dp fixed width)
    WaveformSeekBar(
        progress = progressFlow,
        durationMs = durationFlow.toLong(),
        onDrag = { dragAmount ->
            if (durationFlow > 0 && totalScrollWidth > 0) {
                val progressDelta = -dragAmount / totalScrollWidth
                val newProgress = (progressFlow + progressDelta).coerceIn(0f, 1f)
                val seekTarget = (newProgress * durationFlow).toInt()
                AudioPlayer.seekTo(seekTarget)
            }
        },
        playedColor = playedColor,
        unplayedColor = unplayedColor,
        modifier = Modifier
            .width(70.dp)
            .height(24.dp)
    )

    Spacer(modifier = Modifier.width(12.dp))

    // Duration (12sp size, bold to match Stitch)
    val displaySec = if (isPlaying || positionFlow > 0) currentSec else totalSec
    Text(
        text = String.format(Locale.getDefault(), "%d:%02d", displaySec / 60, displaySec % 60),
        color = textDurationColor,
        fontSize = 12.sp,
        fontWeight = textDurationWeight,
        modifier = Modifier.widthIn(min = 32.dp)
    )
}

// ═══════════════════════════════════════════════════════════
//  InactiveAudioPlayer — not currently playing
// ═══════════════════════════════════════════════════════════

@Composable
fun InactiveAudioPlayer(
    mediaLocalPath: String?,
    mediaUrl: String?,
    mediaType: String,
    isDownloading: Boolean,
    isFailed: Boolean,
    isDownloaded: Boolean,
    onDownloadClick: () -> Unit,
    playedColor: Color,
    unplayedColor: Color,
    textDurationColor: Color,
    textDurationWeight: androidx.compose.ui.text.font.FontWeight,
    buttonBg: Color,
    buttonIconTint: Color,
    localDurationMs: Long
) {
    val context = LocalContext.current

    // Play / Download Button (40dp diameter to match Stitch w-10)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(buttonBg)
            .clickable(enabled = !isDownloading) {
                if (mediaLocalPath != null) {
                    AudioPlayer.play(context, mediaLocalPath)
                } else {
                    onDownloadClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = buttonIconTint,
                strokeWidth = 2.dp
            )
        } else if (isDownloaded) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = buttonIconTint,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                contentDescription = "Download Voice Note",
                tint = buttonIconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Waveform (Exactly 14 bars, 70dp fixed width)
    WaveformSeekBar(
        progress = 0f,
        durationMs = localDurationMs,
        onProgressChange = null,
        playedColor = playedColor,
        unplayedColor = unplayedColor,
        modifier = Modifier
            .width(70.dp)
            .height(24.dp)
    )

    Spacer(modifier = Modifier.width(12.dp))

    // Duration / Status (12sp size, bold to match Stitch)
    val displaySec = localDurationMs / 1000
    val textVal = if (isDownloaded && displaySec > 0) {
        String.format(Locale.getDefault(), "%d:%02d", displaySec / 60, displaySec % 60)
    } else if (isDownloading) {
        "..."
    } else if (isFailed) {
        "!"
    } else {
        "VN"
    }
    Text(
        text = textVal,
        color = textDurationColor,
        fontSize = 12.sp,
        fontWeight = textDurationWeight,
        modifier = Modifier.widthIn(min = 32.dp)
    )
}
