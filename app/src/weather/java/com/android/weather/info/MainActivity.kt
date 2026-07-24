package com.android.weather.info

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.android.weather.info.ui.WeatherViewModel
import com.android.weather.info.ui.screens.ForecastScreen
import com.android.weather.info.ui.screens.SearchScreen
import com.android.weather.info.ui.screens.WeatherScreen
import com.android.weather.info.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            WeatherTheme {
                val navController = rememberNavController()
                val weatherViewModel: WeatherViewModel = viewModel()
                
                NavHost(
                    navController = navController, 
                    startDestination = "weather",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    composable("weather") {
                        WeatherScreen(
                            viewModel = weatherViewModel,
                            onNavigateToSearch = { 
                                navController.navigate("search") {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToFullWeek = { 
                                navController.navigate("forecast") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            viewModel = weatherViewModel,
                            onNavigateBack = { 
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack() 
                                }
                            }
                        )
                    }
                    composable("forecast") {
                        ForecastScreen(
                            viewModel = weatherViewModel,
                            onNavigateBack = { 
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack() 
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    WeatherTheme {
        WeatherScreen()
    }
}