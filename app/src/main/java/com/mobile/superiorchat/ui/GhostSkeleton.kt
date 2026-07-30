package com.mobile.superiorchat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2

/**
 * Standard ghost skeleton loading effect for lightweight layout transition.
 */
fun Modifier.skeletonEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonTranslation"
    )

    this.drawBehind {
        val brush = Brush.linearGradient(
            colors = listOf(
                SurfaceLevel2,
                SurfaceLevel1,
                SurfaceLevel2
            ),
            start = Offset(x = translateAnim, y = translateAnim),
            end = Offset(x = translateAnim + 300f, y = translateAnim + 300f)
        )
        drawRect(brush)
    }
}

// ---------------------------------------------------------------------------
// GENERIC SKELETON COMPONENTS
// ---------------------------------------------------------------------------

@Composable
fun SkeletonTextLine(width: Dp, height: Dp = 16.dp, cornerRadius: Dp = 4.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(cornerRadius))
            .skeletonEffect()
    )
}

@Composable
fun SkeletonAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .skeletonEffect()
    )
}

@Composable
fun SkeletonContainer(modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .skeletonEffect()
    )
}

@Composable
fun SkeletonGalleryItem(modifier: Modifier = Modifier.fillMaxSize()) {
    SkeletonContainer(modifier = modifier, cornerRadius = 0.dp)
}

// ---------------------------------------------------------------------------
// SPECIFIC SCREEN SKELETONS
// ---------------------------------------------------------------------------

@Composable
fun GhostMessageBubble(isFromMe: Boolean) {
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }
    
    // Simulate generic "conversation shape" with random widths mimicking text chunks.
    val randomWidth = remember { (100..250).random().dp }
    
    // Simulate bubble height: mostly 1-liners, occasionally taller chunks.
    val randomHeight = remember { listOf(40.dp, 40.dp, 60.dp, 80.dp, 120.dp).random() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Optional Avatar for received messages
            if (!isFromMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, PrimaryLight.copy(alpha = 0.3f), CircleShape)
                        .skeletonEffect()
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // The Message Bubble Body
            Box(
                modifier = Modifier
                    .width(randomWidth)
                    .height(randomHeight)
                    .clip(shape)
                    .background(if (isFromMe) PrimaryLight.copy(alpha = 0.5f) else SurfaceLevel1)
                    .skeletonEffect()
            )
        }
    }
}

@Composable
fun SkeletonFileListItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonContainer(modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonTextLine(width = 140.dp, height = 16.dp, cornerRadius = 4.dp)
            SkeletonTextLine(width = 90.dp, height = 12.dp, cornerRadius = 4.dp, modifier = Modifier.padding(top = 6.dp))
        }
        SkeletonAvatar(size = 22.dp)
    }
}

@Composable
fun SkeletonStorageLocationItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonAvatar(size = 48.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonTextLine(width = 120.dp, height = 16.dp, cornerRadius = 4.dp)
            SkeletonTextLine(width = 160.dp, height = 12.dp, cornerRadius = 4.dp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
