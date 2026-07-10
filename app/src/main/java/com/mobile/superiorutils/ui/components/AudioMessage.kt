package com.mobile.superiorutils.ui.components

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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorutils.audio.AudioPlayer
import com.mobile.superiorutils.data.entity.MessageStatus
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun WaveformSeekBar(
    progress: Float,
    onProgressChange: ((Float) -> Unit)?,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(
            8, 14, 10, 18, 22, 14, 8, 12, 20, 24,
            16, 10, 14, 22, 18, 8, 14, 16, 10, 20,
            24, 16, 10, 8, 14, 18, 16, 10, 14, 8,
            12, 18, 22, 14, 10, 16, 24, 20, 12, 8,
            10, 16, 22, 18, 14, 12, 8, 14, 20, 16
        )
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .then(
                if (onProgressChange != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val ratio = offset.x / size.width
                                onProgressChange(ratio.coerceIn(0f, 1f))
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        val barWidth = 1.5.dp.toPx()
        val spacing = 1.5.dp.toPx()
        val totalBarWidth = barWidth + spacing
        val maxBars = (size.width / totalBarWidth).toInt().coerceAtMost(barHeights.size)
        
        val centerY = size.height / 2
        val maxVal = barHeights.maxOrNull() ?: 1
        
        for (i in 0 until maxBars) {
            val rawHeight = barHeights[i].dp.toPx()
            val scale = size.height / maxVal.dp.toPx()
            val barHeight = rawHeight * scale * 0.8f
            
            val startX = i * totalBarWidth
            val isPlayed = (i.toFloat() / maxBars) <= progress
            val color = if (isPlayed) tintColor else tintColor.copy(alpha = 0.25f)
            
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(startX, centerY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.8.dp.toPx())
            )
        }
    }
}

@Composable
fun AudioMessage(
    mediaLocalPath: String?,
    mediaUrl: String?,
    mediaType: String,
    status: MessageStatus,
    isFromMe: Boolean,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val currentPath by AudioPlayer.currentPlayingPath.collectAsState()
    val isPlayingFlow by AudioPlayer.isPlaying.collectAsState()
    val isCompletedFlow by AudioPlayer.isCompleted.collectAsState()
    val progressFlow by AudioPlayer.progress.collectAsState()
    val durationFlow by AudioPlayer.durationMs.collectAsState()
    val positionFlow by AudioPlayer.currentPositionMs.collectAsState()

    val isDownloaded = mediaLocalPath != null
    val isCurrent = isDownloaded && currentPath == mediaLocalPath
    val isPlaying = isCurrent && isPlayingFlow
    val isCompleted = isCurrent && isCompletedFlow
    val progress = if (isCurrent) progressFlow else 0f
    
    val totalSec = if (isCurrent && durationFlow > 0) durationFlow / 1000 else 0
    val currentSec = if (isCurrent) positionFlow / 1000 else 0

    val tintColor = if (isFromMe) Color(0xFF1000A9) else Color(0xFFE2E2E2)
    val buttonBg = if (isFromMe) Color.White.copy(alpha = 0.25f) else Color(0xFF24222E)
    val isDownloading = status == MessageStatus.SENDING
    val isFailed = status == MessageStatus.FAILED

    Row(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Control Button
        Box(
            modifier = Modifier
                .size(36.dp)
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
                    modifier = Modifier.size(18.dp),
                    color = tintColor,
                    strokeWidth = 2.dp
                )
            } else if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Replay",
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isDownloaded) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                    contentDescription = "Download Voice Note",
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Progress & Info Area
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
        ) {
            // Waveform
            WaveformSeekBar(
                progress = progress,
                onProgressChange = if (isDownloaded && isCurrent) {
                    { ratio ->
                        val seekTarget = (ratio * durationFlow).toInt()
                        AudioPlayer.seekTo(seekTarget)
                    }
                } else null,
                tintColor = tintColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = if (isDownloaded) Color(0xFF00E676) else tintColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isDownloaded) {
                        if (isCurrent && durationFlow > 0) {
                            String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60)
                        } else {
                            "Voice Note"
                        }
                    } else if (isDownloading) {
                        "Downloading..."
                    } else if (isFailed) {
                        "Download failed. Tap to retry"
                    } else {
                        "Voice Note (${if (mediaType == "audio") "Audio" else "Tap to download"})"
                    },
                    color = tintColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }
    }
}
