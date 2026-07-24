package com.android.weather.info.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.android.weather.info.ui.screens.utils.rememberIsNavigating
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.weather.info.data.WeatherDisplayData
import com.android.weather.info.data.WeatherUiState
import com.android.weather.info.ui.WeatherViewModel
import com.android.weather.info.ui.screens.components.ActionBar
import com.android.weather.info.ui.screens.components.AirQuality
import com.android.weather.info.ui.screens.components.DailyForecast
import com.android.weather.info.ui.screens.components.WeatherShimmer
import com.android.weather.info.ui.screens.components.WeeklyForecast
import com.android.weather.info.ui.theme.ColorBackground
import com.android.weather.info.ui.theme.ColorTextPrimary

internal data class AnimatedForecastState(
    val index: Int,
    val data: WeatherDisplayData
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToFullWeek: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val isSaveSearchEnabled by viewModel.isSaveSearchEnabled.collectAsState()
    val isNavigating = rememberIsNavigating()
    val coroutineScope = rememberCoroutineScope()
    
    val context = LocalContext.current
    val density = LocalDensity.current
    val maxOffsetPx = remember(density) { with(density) { 80.dp.toPx() } }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ColorBackground
    ) { paddingValues ->
        when {
            isNavigating -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    WeatherShimmer()
                }
            }
            uiState is WeatherUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    WeatherShimmer()
                }
            }
            uiState is WeatherUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    ActionBar(
                        cityName = "Offline",
                        isSaveSearchEnabled = isSaveSearchEnabled,
                        onSaveSearchToggle = { viewModel.setSaveSearchEnabled(it) },
                        onSearchClick = onNavigateToSearch
                    )
                    Spacer(modifier = Modifier.height(13.dp))
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { 
                            coroutineScope.launch {
                                viewModel.refresh()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f, fill = false)
                            .clipToBounds()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = (uiState as WeatherUiState.Error).message, color = ColorTextPrimary)
                        }
                    }
                }
            }
            uiState is WeatherUiState.Success -> {
                val data = (uiState as WeatherUiState.Success).data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    ActionBar(
                        cityName = data.cityName,
                        isSaveSearchEnabled = isSaveSearchEnabled,
                        onSaveSearchToggle = { viewModel.setSaveSearchEnabled(it) },
                        onSearchClick = onNavigateToSearch
                    )
                    Spacer(modifier = Modifier.height(13.dp))

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { 
                            coroutineScope.launch {
                                viewModel.refresh()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f, fill = false)
                            .clipToBounds()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            val animatedState = remember(selectedDayIndex, data) {
                                AnimatedForecastState(selectedDayIndex, data)
                            }
                            
                            AnimatedContent(
                                targetState = animatedState,
                                transitionSpec = {
                                    if (targetState.index > initialState.index) {
                                        slideInHorizontally(
                                            animationSpec = tween(300),
                                            initialOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeIn() togetherWith slideOutHorizontally(
                                            animationSpec = tween(300),
                                            targetOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeOut()
                                    } else {
                                        slideInHorizontally(
                                            animationSpec = tween(300),
                                            initialOffsetX = { fullWidth -> -fullWidth }
                                        ) + fadeIn() togetherWith slideOutHorizontally(
                                            animationSpec = tween(300),
                                            targetOffsetX = { fullWidth -> fullWidth }
                                        ) + fadeOut()
                                    }
                                },
                                label = "daily_forecast_transition"
                            ) { target ->
                                Column {
                                    DailyForecast(data = target.data)
                                    Spacer(modifier = Modifier.height(23.dp))
                                    AirQuality(data = target.data)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            WeeklyForecast(
                                data = data,
                                onDaySelected = viewModel::selectDay,
                                onFullWeekClick = onNavigateToFullWeek
                            )
                        }
                    }
                }
            }
        }
    }
}