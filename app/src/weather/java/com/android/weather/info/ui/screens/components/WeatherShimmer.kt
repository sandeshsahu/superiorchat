package com.android.weather.info.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import com.android.weather.info.ui.theme.ColorSurface

@Composable
fun WeatherShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val shimmerColor = ColorSurface.copy(alpha = alpha.value)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        // Action Bar Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50)).background(shimmerColor))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.height(24.dp).width(120.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
            }
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(shimmerColor))
        }

        Spacer(modifier = Modifier.height(37.dp)) // 13 + 24 top margin for daily forecast

        // Daily Forecast Shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(shimmerColor)
        )

        Spacer(modifier = Modifier.height(23.dp))

        // Air Quality / Details Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(shimmerColor)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(shimmerColor)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Forecast Shimmer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.height(24.dp).width(150.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
            Box(modifier = Modifier.height(16.dp).width(60.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly Forecast Items Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(65.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(50))
                        .background(shimmerColor)
                )
            }
        }
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_mod")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_mod_alpha"
    )
    return this.drawWithContent {
        drawContent()
        drawRect(color = ColorSurface.copy(alpha = alpha.value))
    }
}
