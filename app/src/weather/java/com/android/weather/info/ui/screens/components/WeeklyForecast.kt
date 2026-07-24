package com.android.weather.info.ui.screens.components

import com.mobile.superiorchat.R
import com.android.weather.info.ui.screens.utils.ForecastData
import com.android.weather.info.ui.screens.utils.ForecastItem
import com.android.weather.info.ui.screens.utils.fromHex
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.weather.info.ui.theme.ColorGradient1
import com.android.weather.info.ui.theme.ColorGradient2
import com.android.weather.info.ui.theme.ColorGradient3
import com.android.weather.info.ui.theme.ColorTextAction
import com.android.weather.info.ui.theme.ColorTextPrimary
import com.android.weather.info.ui.theme.ColorTextPrimaryVariant
import com.android.weather.info.ui.theme.ColorTextSecondary
import com.android.weather.info.ui.theme.ColorTextSecondaryVariant
import com.android.weather.info.data.WeatherDisplayData

@Composable
fun WeeklyForecast(
    data: WeatherDisplayData,
    onDaySelected: (Int) -> Unit = {},
    onFullWeekClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WeeklyForecastHeader(onFullWeekClick = onFullWeekClick)
        
        // Use Row with horizontalScroll instead of LazyRow for 7 items to completely eliminate recomposition lag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            data.weeklyForecast.forEachIndexed { index, item ->
                Forecast(
                    item = item,
                    modifier = Modifier.clickable { onDaySelected(index) }
                )
            }
        }
    }
}

@Composable
fun WeeklyForecastHeader(
    onFullWeekClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Weekly Forecast",
            style = MaterialTheme.typography.titleLarge,
            color = ColorTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        if (onFullWeekClick != null) {
            ActionText(
                modifier = Modifier.clickable { onFullWeekClick() }
            )
        }
    }
}

@Composable
fun ActionText(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Full Week",
            style = MaterialTheme.typography.titleSmall,
            color = ColorTextAction,
            fontWeight = FontWeight.Medium
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ColorTextAction,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun Forecast(
    modifier: Modifier = Modifier,
    item: ForecastItem
) {
    val isSelected = item.isSelected
    
    val selectedBrush = remember {
        Brush.linearGradient(
            0f to ColorGradient1,
            0.5f to ColorGradient2,
            1f to ColorGradient3
        )
    }

    val backgroundModifier = if (isSelected) {
        Modifier.background(
            shape = RoundedCornerShape(50),
            brush = selectedBrush
        )
    } else {
        Modifier
    }
    
    val primaryTextColor = remember(isSelected) {
        if (isSelected) ColorTextSecondary else ColorTextPrimary
    }
    val secondaryTextColor = remember(isSelected) {
        if (isSelected) ColorTextSecondaryVariant else ColorTextPrimaryVariant
    }
    val temperatureTextStyle = remember(isSelected) {
        if (isSelected) {
            TextStyle(
                brush = Brush.verticalGradient(
                    0f to Color.White,
                    1f to Color.White.copy(alpha = 0.3f)
                ),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            TextStyle(
                color = ColorTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }

    Column(
        modifier = modifier
            .then(backgroundModifier)
            .width(65.dp)
            .padding(
                horizontal = 10.dp,
                vertical = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.dayOfWeek,
            style = MaterialTheme.typography.labelLarge,
            color = primaryTextColor
        )
        Text(
            text = item.date,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
            color = secondaryTextColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        WeatherImage(image = item.image)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.temperature,
            style = temperatureTextStyle
        )
        Spacer(modifier = Modifier.height(8.dp))
        AirQualityIndicator(
            value = item.airQuality,
            color = item.airQualityIndicatorColorHex
        )
    }
}

@Composable
fun WeatherImage(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AirQualityIndicator(
    modifier: Modifier = Modifier,
    value: String,
    color: String
) {
    val parsedColor = remember(color) { Color.fromHex(color) }

    Surface(
        modifier = modifier,
        color = parsedColor.copy(alpha = 0.2f),
        contentColor = parsedColor,
        shape = RoundedCornerShape(50)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}