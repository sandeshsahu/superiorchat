package com.android.weather.info.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.weather.info.data.GeoResult
import com.android.weather.info.data.HourlyForecastItem
import com.android.weather.info.data.WeatherDisplayData
import com.android.weather.info.data.WeatherRepository
import com.android.weather.info.data.WeatherResponse
import com.android.weather.info.data.WeatherUiState
import com.android.weather.info.data.local.WeatherLocalStorage
import com.android.weather.info.ui.screens.utils.ForecastItem
import com.android.weather.info.ui.screens.utils.getWeatherDescriptionForCode
import com.android.weather.info.ui.screens.utils.getWeatherIconForCode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(FlowPreview::class)
class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val localStorage = WeatherLocalStorage(application)
    private val repository = WeatherRepository(localStorage)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeoResult>>(emptyList())
    val searchResults: StateFlow<List<GeoResult>> = _searchResults.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()

    private val _currentCityNameFlow = MutableStateFlow("")
    val currentCityNameFlow: StateFlow<String> = _currentCityNameFlow.asStateFlow()

    private val _isSaveSearchEnabled = MutableStateFlow(true)
    val isSaveSearchEnabled: StateFlow<Boolean> = _isSaveSearchEnabled.asStateFlow()

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var currentCityName: String = ""
    private var currentTimezone: String = "auto"
    
    private var rawWeatherResponse: WeatherResponse? = null
    private var searchJob: Job? = null
    
    private var cachedNearbyCities: List<GeoResult>? = null
    
    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    val defaultSuggestedCities = listOf(
        GeoResult(1, "Mumbai", "India", "Maharashtra", 19.0760, 72.8777, "Asia/Kolkata"),
        GeoResult(2, "Delhi", "India", "Delhi", 28.6139, 77.2090, "Asia/Kolkata"),
        GeoResult(3, "Bengaluru", "India", "Karnataka", 12.9716, 77.5946, "Asia/Kolkata"),
        GeoResult(4, "London", "United Kingdom", "England", 51.5074, -0.1278, "Europe/London"),
        GeoResult(5, "New York", "United States", "New York", 40.7128, -74.0060, "America/New_York")
    )

    init {
        _isSaveSearchEnabled.value = localStorage.isSaveSearchEnabled()
        loadInitialLocationAndWeather()

        // Debounce search queries with job cancellation and loading state
        viewModelScope.launch {
            searchQuery.collect { query ->
                searchJob?.cancel()
                val trimmed = query.trim()
                if (trimmed.length >= 2) {
                    _isSearchLoading.value = true
                    searchJob = launch {
                        kotlinx.coroutines.delay(300)
                        performSearch(trimmed)
                    }
                } else {
                    _isSearchLoading.value = false
                    populateNearbyCities()
                }
            }
        }
    }

    fun populateNearbyCities() {
        viewModelScope.launch {
            if (cachedNearbyCities != null) {
                _searchResults.value = cachedNearbyCities!!.take(5)
                return@launch
            }
            if (currentLat != 0.0 && currentLon != 0.0) {
                val result = repository.getNearbyCities(currentLat, currentLon)
                if (result.isSuccess) {
                    val results = result.getOrNull()
                    if (!results.isNullOrEmpty()) {
                        val topResults = results.take(5)
                        cachedNearbyCities = topResults
                        _searchResults.value = topResults
                        return@launch
                    }
                }
            }
            _searchResults.value = defaultSuggestedCities.take(5)
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun setSaveSearchEnabled(enabled: Boolean) {
        _isSaveSearchEnabled.value = enabled
        localStorage.setSaveSearchEnabled(enabled)
    }

    private fun loadInitialLocationAndWeather() {
        viewModelScope.launch {
            val cachedWeather = repository.getCachedWeather()
            val locationResult = repository.getInitialLocation()
            
            if (locationResult.isSuccess) {
                val loc = locationResult.getOrNull()!!
                currentLat = loc.latitude
                currentLon = loc.longitude
                currentCityName = loc.name
                _currentCityNameFlow.value = loc.name
                currentTimezone = loc.timezone ?: "auto"
                
                if (cachedWeather != null) {
                    rawWeatherResponse = cachedWeather
                    val newData = computeDisplayData(_selectedDayIndex.value)
                    if (newData != null) {
                        _uiState.value = WeatherUiState.Success(newData)
                    }
                } else {
                    _uiState.value = WeatherUiState.Loading
                }
                
                populateNearbyCities()
                fetchWeather(silent = cachedWeather != null)
            } else {
                if (cachedWeather != null) {
                    rawWeatherResponse = cachedWeather
                    val newData = computeDisplayData(_selectedDayIndex.value)
                    if (newData != null) {
                        _uiState.value = WeatherUiState.Success(newData)
                    }
                    _toastMessage.value = "Offline. Displaying saved weather."
                } else {
                    _uiState.value = WeatherUiState.Error("Failed to determine location. Please search for a city.")
                }
            }
        }
    }

    private suspend fun fetchWeather(silent: Boolean = false) {
        val weatherResult = repository.getWeather(currentLat, currentLon, currentTimezone)
        if (weatherResult.isSuccess) {
            val response = weatherResult.getOrNull()
            rawWeatherResponse = response
            
            val newData = computeDisplayData(_selectedDayIndex.value)
            if (newData != null) {
                _uiState.value = WeatherUiState.Success(newData)
                localStorage.saveLatestWeatherStrings(newData.temperature, newData.weatherDescription, newData.humidity)
            }
        } else {
            if (rawWeatherResponse != null) {
                // Keep showing cached UI, just alert user about offline status
                _toastMessage.value = "No internet connection. Showing saved weather."
            } else {
                _uiState.value = WeatherUiState.Error("No internet connection and no saved data available.\n\nIf you are online try refreshing the page\nBy swiping down to screen")
            }
        }
    }

    private val _isTrackingCurrentLocation = MutableStateFlow(true)
    val isTrackingCurrentLocation: StateFlow<Boolean> = _isTrackingCurrentLocation.asStateFlow()

    suspend fun refresh() {
        _isRefreshing.value = true
        try {
            val shouldFetchIp = if (_isSaveSearchEnabled.value) {
                !localStorage.hasSavedLocation()
            } else {
                true
            }

            if (shouldFetchIp) {
                _isTrackingCurrentLocation.value = true
                val locationResult = repository.getInitialLocation(forceRefresh = true)
                if (locationResult.isSuccess) {
                    val loc = locationResult.getOrNull()!!
                    currentLat = loc.latitude
                    currentLon = loc.longitude
                    currentCityName = loc.name
                    _currentCityNameFlow.value = loc.name
                    currentTimezone = loc.timezone ?: "auto"
                    localStorage.saveLocation(currentLat, currentLon, currentCityName, currentTimezone)
                }
            } else {
                _isTrackingCurrentLocation.value = false
                currentLat = localStorage.getLat()
                currentLon = localStorage.getLon()
                currentCityName = localStorage.getCityName()
                _currentCityNameFlow.value = currentCityName
                currentTimezone = localStorage.getTimezone()
            }
            fetchWeather(silent = true)
        } finally {
            _isRefreshing.value = false
        }
    }

    fun onCitySelected(city: GeoResult) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _isTrackingCurrentLocation.value = false
            currentLat = city.latitude
            currentLon = city.longitude
            currentCityName = city.name
            _currentCityNameFlow.value = city.name
            currentTimezone = city.timezone ?: "auto"
            
            _selectedDayIndex.value = 0
            
            localStorage.saveLocation(currentLat, currentLon, currentCityName, currentTimezone)
            localStorage.clearWeatherJson()
            rawWeatherResponse = null
            
            searchQuery.value = ""
            populateNearbyCities()
            fetchWeather(silent = false)
        }
    }

    fun resumeTracking() {
        if (_isTrackingCurrentLocation.value) return
        _isTrackingCurrentLocation.value = true
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            refresh()
        }
    }

    fun selectDay(index: Int) {
        viewModelScope.launch {
            val newData = computeDisplayData(index)
            if (newData != null) {
                // Emit both states simultaneously on the same frame to prevent animation desync
                _selectedDayIndex.value = index
                _uiState.value = WeatherUiState.Success(newData)
            } else {
                _selectedDayIndex.value = index
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _isSearchLoading.value = true
        try {
            val result = repository.searchCity(query)
            if (result.isSuccess) {
                val results = result.getOrNull()
                _searchResults.value = if (!results.isNullOrEmpty()) results else emptyList()
            } else {
                _searchResults.value = emptyList()
            }
        } finally {
            _isSearchLoading.value = false
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
        populateNearbyCities()
    }

    private suspend fun computeDisplayData(selectedIdx: Int): WeatherDisplayData? {
        val response = rawWeatherResponse ?: return null
        val current = response.current_weather
        val daily = response.daily ?: return null
        val hourly = response.hourly
        
        if (daily.time.isEmpty()) return null

        val actualTodayIndex = if (current?.time != null) {
            val currentDateStr = current.time.split("T").first()
            daily.time.indexOfFirst { it.startsWith(currentDateStr) }.coerceAtLeast(0)
        } else 0

        val safeUiIdx = if (selectedIdx in 0 until (daily.time.size - actualTodayIndex)) selectedIdx else 0
        val safeApiIdx = actualTodayIndex + safeUiIdx

        val displayData = withContext(Dispatchers.Default) {
            // Parse date for the selected day
            val dayDateString = try {
                val date = LocalDate.parse(daily.time[safeApiIdx])
                val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
                val dayOfMonth = date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
                "$dayOfWeek $dayOfMonth"
            } catch (e: Exception) {
                daily.time[safeApiIdx]
            }

            // Build 7-day forecast list starting from today
            val validIndices = (actualTodayIndex until daily.time.size).take(7)
            val forecastItems = validIndices.mapIndexed { uiIndex, apiIndex ->
                val (dateStr, dayOfWeekStr) = try {
                    val d = LocalDate.parse(daily.time[apiIndex])
                    Pair(
                        d.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())),
                        d.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                    )
                } catch(e: Exception) { Pair("", "") }

                val maxTempInt = daily.temperature_2m_max.getOrNull(apiIndex)?.toInt() ?: 0
                val displayTempInt = if (uiIndex == 0 && current?.temperature != null) current.temperature.toInt() else maxTempInt
                val displayTemp = "$displayTempInt°"
                val code = if (uiIndex == 0 && current?.weathercode != null) current.weathercode else daily.weathercode.getOrNull(apiIndex) ?: 0
                
                val colorHex = if (uiIndex % 2 == 0) "#FF9800" else "#4CAF50"
                val aq = if (uiIndex % 2 == 0) "AQI 45" else "AQI 20"
                val isDay = if (uiIndex == 0 && current?.is_day != null) current.is_day == 1 else true

                ForecastItem(
                    dayOfWeek = dayOfWeekStr,
                    date = dateStr,
                    image = getWeatherIconForCode(code, isDay),
                    temperature = displayTemp,
                    airQualityIndicatorColorHex = colorHex,
                    airQuality = aq,
                    isSelected = (uiIndex == safeUiIdx)
                )
            }

            val isToday = (safeUiIdx == 0)

            val minTemp = daily.temperature_2m_min.getOrNull(safeApiIdx)?.toInt() ?: 0
            val maxTemp = daily.temperature_2m_max.getOrNull(safeApiIdx)?.toInt() ?: 0

            val curTemp = if (isToday && current?.temperature != null) {
                current.temperature.toInt()
            } else {
                maxTemp
            }

            val code = if (isToday && current?.weathercode != null) {
                current.weathercode
            } else {
                daily.weathercode.getOrNull(safeApiIdx) ?: 0
            }

            val feelsLikeText = if (isToday) {
                "Feels like $minTemp°"
            } else {
                "Low $minTemp° / High $maxTemp°"
            }

            val wind = current?.windspeed ?: 10.0

            val currentHour = if (isToday && current?.time != null) {
                current.time.split("T").last().split(":").firstOrNull()?.toIntOrNull() ?: 0
            } else 0

            // Hourly index offset for selected day (only future hours for Today)
            val hourlyItems = (currentHour until 24).map { h ->
                val idx = safeApiIdx * 24 + h
                val rawTime = hourly?.time?.getOrNull(idx) ?: ""
                val timeStr = if (rawTime.contains("T")) {
                    val timePart = rawTime.split("T").last()
                    val hour = timePart.split(":").firstOrNull()?.toIntOrNull() ?: h
                    val amPm = if (hour < 12) "AM" else "PM"
                    val hour12 = if (hour % 12 == 0) 12 else hour % 12
                    "$hour12:00 $amPm"
                } else {
                    val amPm = if (h < 12) "AM" else "PM"
                    val hour12 = if (h % 12 == 0) 12 else h % 12
                    "$hour12:00 $amPm"
                }
                val tempVal = hourly?.temperature_2m?.getOrNull(idx)?.toInt() ?: 0
                val codeVal = hourly?.weathercode?.getOrNull(idx) ?: 0
                val humVal = hourly?.relativehumidity_2m?.getOrNull(idx) ?: 0
                val rainVal = hourly?.precipitation_probability?.getOrNull(idx) ?: 0
                val windVal = hourly?.windspeed_10m?.getOrNull(idx) ?: 0.0
                val isHourlyDay = hourly?.is_day?.getOrNull(idx) == 1
                
                HourlyForecastItem(
                    time = timeStr,
                    temperature = "$tempVal°",
                    weatherIcon = getWeatherIconForCode(codeVal, isHourlyDay),
                    weatherDescription = getWeatherDescriptionForCode(codeVal),
                    humidity = "$humVal%",
                    rainChance = "$rainVal%",
                    windSpeed = "$windVal km/h"
                )
            }

            val hourlyIdx = safeApiIdx * 24
            val humidityVal = hourly?.relativehumidity_2m?.getOrNull(hourlyIdx) ?: 50

            val isCurrentDay = if (isToday && current?.is_day != null) current.is_day == 1 else true

            WeatherDisplayData(
                cityName = currentCityName,
                temperature = "$curTemp°",
                feelsLike = feelsLikeText,
                weatherDescription = getWeatherDescriptionForCode(code),
                weatherIcon = getWeatherIconForCode(code, isCurrentDay),
                windSpeed = "$wind km/h",
                humidity = "$humidityVal%",
                uvIndex = daily.uv_index_max.getOrNull(safeApiIdx)?.toString() ?: "0.0",
                rainChance = "${daily.precipitation_probability_max.getOrNull(safeApiIdx) ?: 0}%",
                sunrise = daily.sunrise.getOrNull(safeApiIdx)?.split("T")?.last() ?: "06:00",
                dateString = dayDateString,
                weeklyForecast = forecastItems,
                hourlyForecastForSelectedDay = hourlyItems
            )
        }

        return displayData
    }
}
