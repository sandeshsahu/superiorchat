package com.mobile.superiorutils.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun WaveformSeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val barHeights = remember {
        listOf(
            15, 25, 20, 30, 40, 25, 15, 20, 35, 45,
            30, 20, 25, 40, 35, 15, 25, 30, 20, 40,
            45, 30, 20, 15, 25, 35, 30, 20, 25, 15
        )
    }

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val ratio = offset.x / size.width
                        onProgressChange(ratio.coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val totalBarWidth = barWidth + spacing
        val maxBars = (size.width / totalBarWidth).toInt().coerceAtMost(barHeights.size)
        
        val centerY = size.height / 2
        
        for (i in 0 until maxBars) {
            val barHeight = barHeights[i].dp.toPx()
            val startX = i * totalBarWidth
            val isPlayed = (i.toFloat() / maxBars) <= progress
            val color = if (isPlayed) tintColor else tintColor.copy(alpha = 0.3f)
            
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(startX, centerY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMessage(
    mediaLocalPath: String,
    isFromMe: Boolean
) {
    val context = LocalContext.current
    val currentPath by AudioPlayer.currentPlayingPath.collectAsState()
    val isPlayingFlow by AudioPlayer.isPlaying.collectAsState()
    val progressFlow by AudioPlayer.progress.collectAsState()
    val durationFlow by AudioPlayer.durationMs.collectAsState()
    val positionFlow by AudioPlayer.currentPositionMs.collectAsState()

    val isCurrent = currentPath == mediaLocalPath
    val isPlaying = isCurrent && isPlayingFlow
    val progress = if (isCurrent) progressFlow else 0f
    
    val totalSec = if (isCurrent && durationFlow > 0) durationFlow / 1000 else 0
    val currentSec = if (isCurrent) positionFlow / 1000 else 0

    val tintColor = if (isFromMe) Color(0xFF1000A9) else Color(0xFFE2E2E2)
    val buttonBg = if (isFromMe) Color.White.copy(alpha = 0.2f) else Color(0xFF24222E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                AudioPlayer.play(context, mediaLocalPath)
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(buttonBg)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause Voice Note",
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            WaveformSeekBar(
                progress = progress,
                onProgressChange = {
                    if (isCurrent) {
                        val seekTarget = (it * durationFlow).toInt()
                        AudioPlayer.seekTo(seekTarget)
                    }
                },
                tintColor = tintColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d / %02d:%02d",
                    currentSec / 60, currentSec % 60,
                    totalSec / 60, totalSec % 60
                ),
                color = tintColor.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}
