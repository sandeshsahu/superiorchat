package com.android.weather.info.data

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

// --- Open-Meteo Weather API Models ---
data class WeatherResponse(
    val current_weather: CurrentWeather?,
    val daily: DailyForecast?,
    val hourly: HourlyData?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val is_day: Int?,
    val time: String
)

data class DailyForecast(
    val time: List<String>,           // e.g. "2025-07-23"
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weathercode: List<Int>,
    val precipitation_probability_max: List<Int>,
    val uv_index_max: List<Double>,
    val sunrise: List<String>
)

data class HourlyData(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val weathercode: List<Int>?,
    val relativehumidity_2m: List<Int>?,
    val precipitation_probability: List<Int>?,
    val windspeed_10m: List<Double>?,
    val is_day: List<Int>?
)

@Immutable
data class HourlyForecastItem(
    val time: String,
    val temperature: String,
    val weatherIcon: Int,
    val weatherDescription: String,
    val humidity: String,
    val rainChance: String,
    val windSpeed: String
)

// --- Open-Meteo Geocoding API Models ---
data class GeocodingResponse(
    val results: List<GeoResult>?
)

data class GeoResult(
    val id: Int,
    val name: String,
    val country: String?,
    val admin1: String?, // state/province
    val latitude: Double,
    val longitude: Double,
    val timezone: String?
)

// --- IP Location API Models ---
data class IpLocationResponse(
    val status: String?,
    val country: String?,
    val city: String?,
    val lat: Double?,
    val lon: Double?,
    val timezone: String?
)

// --- CountriesDev API Models (For Nearest Cities) ---
data class CountriesDevCity(
    val geonameId: Long,
    val name: String,
    val countryCode: String,
    val admin1Code: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?
)

// --- Unified App UI State Models ---
sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherDisplayData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@Immutable
data class WeatherDisplayData(
    val cityName: String,
    val temperature: String,
    val feelsLike: String,
    val weatherDescription: String,
    val weatherIcon: Int,
    val windSpeed: String,
    val humidity: String,
    val uvIndex: String,
    val rainChance: String,
    val sunrise: String,
    val dateString: String,
    val weeklyForecast: List<com.android.weather.info.ui.screens.utils.ForecastItem>,
    val hourlyForecastForSelectedDay: List<HourlyForecastItem> = emptyList(),
    val isUsingIpLocation: Boolean = false
)
