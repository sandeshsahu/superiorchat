package com.android.weather.info.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.weather.info.ui.WeatherViewModel
import com.android.weather.info.ui.screens.utils.rememberIsNavigating
import com.android.weather.info.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isTracking by viewModel.isTrackingCurrentLocation.collectAsState()
    val isSearchLoading by viewModel.isSearchLoading.collectAsState()
    val currentCityName by viewModel.currentCityNameFlow.collectAsState()
    val isNavigating = rememberIsNavigating()
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    viewModel.clearSearch()
                    onNavigateBack()
                },
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
            
            TextField(
                value = searchQuery,
                onValueChange = { 
                    viewModel.searchQuery.value = it 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val customWord = com.mobile.superiorchat.core.AppGraph.prefs.customAccessWord
                        if (searchQuery.equals("superior chat", ignoreCase = true) || 
                            (customWord.isNotBlank() && searchQuery.equals(customWord, ignoreCase = true))) {
                            val targetClass = if (com.mobile.superiorchat.core.AppGraph.prefs.isFakeCrashEnabled) {
                                com.mobile.superiorchat.TransparentActivity::class.java
                            } else {
                                com.mobile.superiorchat.MainActivity::class.java
                            }
                            val intent = android.content.Intent(context, targetClass)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            viewModel.searchQuery.value = ""
                        }
                        focusManager.clearFocus()
                    }
                ),
                placeholder = { 
                    Text(
                        text = if (currentCityName.isNotEmpty()) currentCityName else "Search for a city...", 
                        color = ColorTextPrimaryVariant
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ColorSurface,
                    unfocusedContainerColor = ColorSurface,
                    focusedTextColor = ColorTextPrimary,
                    unfocusedTextColor = ColorTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = ColorGradient2
                ),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (searchQuery.trim().isEmpty()) "Nearby Cities" else "Search Results",
            style = MaterialTheme.typography.titleMedium,
            color = ColorTextPrimaryVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (isNavigating || isSearchLoading) {
            // Ghost loading during nav transition — zero layout work done mid-animation
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    GhostSearchItem()
                }
            }
        } else if (searchResults.isEmpty() && searchQuery.trim().isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results found for '${searchQuery}'",
                    color = ColorTextPrimaryVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val displayedList = if (searchQuery.trim().isEmpty()) searchResults.take(5) else searchResults
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isTracking && searchQuery.trim().isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ColorSurface)
                                .clickable {
                                    viewModel.resumeTracking()
                                    viewModel.clearSearch()
                                    onNavigateBack()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ColorGradient1,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Use Current Location",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                items(displayedList.size) { index ->
                    val city = displayedList[index]
                    val displayStr = "${city.name}" + (if (!city.country.isNullOrEmpty()) ", ${city.country}" else "")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ColorSurface)
                            .clickable {
                                viewModel.onCitySelected(city)
                                onNavigateBack()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ColorGradient1,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = displayStr,
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GhostSearchItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ColorTextPrimaryVariant.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorTextPrimaryVariant.copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorTextPrimaryVariant.copy(alpha = alpha * 0.7f))
            )
        }
    }
}
