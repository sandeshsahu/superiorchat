package com.android.weather.info.ui.screens.components

import com.mobile.superiorchat.R
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import com.android.weather.info.ui.theme.ColorGradient1
import com.android.weather.info.ui.theme.ColorGradient2
import com.android.weather.info.ui.theme.ColorGradient3
import com.android.weather.info.ui.theme.ColorTextSecondary
import com.android.weather.info.ui.theme.ColorTextSecondaryVariant
import com.android.weather.info.ui.theme.ColorWindForecast
import com.android.weather.info.data.WeatherDisplayData

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DailyForecast(
    data: WeatherDisplayData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Background Card
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .matchParentSize()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Image, Title, Date
            Column {
                AsyncImage(
                    model = data.weatherIcon,
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(175.dp)
                        .padding(start = 4.dp)
                )
                Text(
                    text = data.weatherDescription,
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorTextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 24.dp)
                )
                Text(
                    text = data.dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondaryVariant,
                    modifier = Modifier.padding(start = 24.dp, bottom = 24.dp)
                )
            }

            // Right Side: Temperature and Wind
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 24.dp)
            ) {
                // ForecastValue centered relative to image (approx 175dp height)
                Box(
                    modifier = Modifier.height(175.dp).padding(top = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    ForecastValue(
                        degree = data.temperature,
                        description = data.feelsLike
                    )
                }
                
                // WindImage aligned roughly with title
                WindForecastImage(
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CardBackground (
    modifier: Modifier = Modifier
) {
    val cardBrush = remember {
        Brush.linearGradient(
            0f to ColorGradient1,
            0.5f to ColorGradient2,
            1f to ColorGradient3
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = cardBrush,
                shape = RoundedCornerShape(32.dp)
            )
    )
}

@Composable
fun ForecastValue (
    modifier: Modifier = Modifier,
    degree: String = "29°",
    description: String = "Feels like 32°"
) {
    val gradientBrush = remember {
        Brush.linearGradient(
            0f to Color.White,
            1f to Color.White.copy(alpha = 0.3f)
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = degree,
                style = TextStyle (
                    brush = gradientBrush,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTextSecondaryVariant
        )
    }
}

@Composable
fun WindForecastImage (
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_frosty),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = ColorWindForecast
        )
        Icon(
            painter = painterResource(R.drawable.ic_wind),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = ColorWindForecast
        )
    }
}