package com.android.weather.info.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.android.weather.info.ui.screens.utils.rememberIsNavigating
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import com.android.weather.info.ui.screens.components.shimmerEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.weather.info.data.HourlyForecastItem
import com.android.weather.info.data.WeatherUiState
import com.android.weather.info.ui.WeatherViewModel
import com.android.weather.info.ui.screens.components.ForecastShimmer
import com.android.weather.info.ui.screens.components.WeeklyForecast
import com.android.weather.info.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForecastScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val isNavigating = rememberIsNavigating()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ColorSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ColorTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "7-Day & Hourly Forecast",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (uiState is WeatherUiState.Success) {
                    val cityName = (uiState as WeatherUiState.Success).data.cityName
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextPrimaryVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isNavigating -> {
                // Defer heavy rendering until slide animation finishes (DRY ghost loading)
                Box(modifier = Modifier.fillMaxSize()) {
                    ForecastShimmer()
                }
            }
            uiState is WeatherUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    ForecastShimmer()
                }
            }
            uiState is WeatherUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = (uiState as WeatherUiState.Error).message,
                        color = ColorTextPrimary
                    )
                }
            }
            uiState is WeatherUiState.Success -> {
                val data = (uiState as WeatherUiState.Success).data

                // Top: Weekly Forecast selector
                WeeklyForecast(
                    data = data,
                    onDaySelected = viewModel::selectDay,
                    onFullWeekClick = null
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hourly Forecast Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hourly Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = data.dateString,
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorTextAction,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = data.hourlyForecastForSelectedDay,
                        key = { it.time + "_" + selectedDayIndex },
                        contentType = { "HourlyCard" }
                    ) { hourlyItem ->
                        HourlyCard(item = hourlyItem)
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyCard(
    item: HourlyForecastItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ColorSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        // Time & Description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ColorBackground,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            SubcomposeAsyncImage(
                model = item.weatherIcon,
                contentDescription = null,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                },
                modifier = Modifier.size(36.dp)
            )

            Column {
                Text(
                    text = item.weatherDescription,
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Rain: ${item.rainChance} | Wind: ${item.windSpeed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextPrimaryVariant
                )
            }
        }

        // Temperature
        Text(
            text = item.temperature,
            style = MaterialTheme.typography.titleLarge,
            color = ColorTextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp
        )
    }
}
}
