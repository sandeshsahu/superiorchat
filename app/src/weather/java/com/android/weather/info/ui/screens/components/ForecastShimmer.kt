package com.android.weather.info.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.android.weather.info.ui.theme.ColorSurface

@Composable
fun ForecastShimmer(modifier: Modifier = Modifier) {
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
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
    ) {
        // Header Shimmer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(shimmerColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.height(24.dp).width(180.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
                Box(modifier = Modifier.height(16.dp).width(100.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        // Hourly Forecast Header Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.height(24.dp).width(150.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
            Box(modifier = Modifier.height(16.dp).width(80.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor))
        }

        Spacer(modifier = Modifier.height(12.dp))

        HourlyListShimmer()
    }
}

@Composable
fun HourlyListShimmer(modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(shimmerColor)
            )
        }
    }
}
