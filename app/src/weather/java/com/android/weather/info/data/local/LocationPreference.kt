package com.android.weather.info.data.local

import android.content.Context
import android.content.SharedPreferences

class LocationPreference(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    fun hasSavedLocation(): Boolean {
        return prefs.contains(KEY_LAT) && prefs.contains(KEY_LON)
    }

    fun isSaveSearchEnabled(): Boolean {
        return prefs.getBoolean(KEY_SAVE_SEARCH, true)
    }

    fun setSaveSearchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_SEARCH, enabled).apply()
    }

    fun saveLocation(lat: Double, lon: Double, cityName: String, timezone: String = "auto") {
        prefs.edit().apply {
            putString(KEY_LAT, lat.toString())
            putString(KEY_LON, lon.toString())
            putString(KEY_CITY, cityName)
            putString(KEY_TIMEZONE, timezone)
            apply()
        }
    }

    fun getLat(): Double = prefs.getString(KEY_LAT, "0.0")?.toDoubleOrNull() ?: 0.0
    fun getLon(): Double = prefs.getString(KEY_LON, "0.0")?.toDoubleOrNull() ?: 0.0
    fun getCityName(): String = prefs.getString(KEY_CITY, "Unknown") ?: "Unknown"
    fun getTimezone(): String = prefs.getString(KEY_TIMEZONE, "auto") ?: "auto"

    fun saveWeatherResponse(json: String) {
        prefs.edit().putString(KEY_WEATHER_JSON, json).apply()
    }

    fun getWeatherResponse(): String? = prefs.getString(KEY_WEATHER_JSON, null)

    companion object {
        private const val KEY_LAT = "saved_lat"
        private const val KEY_LON = "saved_lon"
        private const val KEY_CITY = "saved_city"
        private const val KEY_TIMEZONE = "saved_timezone"
        private const val KEY_WEATHER_JSON = "saved_weather_json"
        private const val KEY_SAVE_SEARCH = "save_search_enabled"
    }
}
