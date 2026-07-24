package com.android.weather.info.data.local

import android.content.Context
import android.content.SharedPreferences

class WeatherLocalStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_local_storage", Context.MODE_PRIVATE)

    fun hasSavedLocation(): Boolean {
        return prefs.contains(KEY_LAT) && prefs.contains(KEY_LON)
    }

    fun isSaveSearchEnabled(): Boolean = prefs.getBoolean(KEY_SAVE_SEARCH_ENABLED, true)

    fun setSaveSearchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_SEARCH_ENABLED, enabled).apply()
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

    fun getLat(): Double = prefs.getString(KEY_LAT, DEFAULT_MUMBAI_LAT.toString())?.toDoubleOrNull() ?: DEFAULT_MUMBAI_LAT
    fun getLon(): Double = prefs.getString(KEY_LON, DEFAULT_MUMBAI_LON.toString())?.toDoubleOrNull() ?: DEFAULT_MUMBAI_LON
    fun getCityName(): String = prefs.getString(KEY_CITY, DEFAULT_MUMBAI_NAME) ?: DEFAULT_MUMBAI_NAME
    fun getTimezone(): String = prefs.getString(KEY_TIMEZONE, DEFAULT_MUMBAI_TZ) ?: DEFAULT_MUMBAI_TZ

    fun saveWeatherJson(json: String) {
        prefs.edit().apply {
            putString(KEY_WEATHER_JSON, json)
            putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun getWeatherJson(): String? = prefs.getString(KEY_WEATHER_JSON, null)
    fun getLastFetchTime(): Long = prefs.getLong(KEY_LAST_FETCH_TIME, 0L)

    fun clearWeatherJson() {
        prefs.edit().remove(KEY_WEATHER_JSON).apply()
    }

    fun saveLatestWeatherStrings(temp: String, condition: String, humidity: String) {
        prefs.edit().apply {
            putString(KEY_LAST_TEMP, temp)
            putString(KEY_LAST_CONDITION, condition)
            putString(KEY_LAST_HUMIDITY, humidity)
            apply()
        }
    }

    fun getLastTemp(): String = prefs.getString(KEY_LAST_TEMP, "--") ?: "--"
    fun getLastCondition(): String = prefs.getString(KEY_LAST_CONDITION, "Unknown") ?: "Unknown"
    fun getLastHumidity(): String = prefs.getString(KEY_LAST_HUMIDITY, "--") ?: "--"

    companion object {
        private const val KEY_LAT = "saved_lat"
        private const val KEY_LON = "saved_lon"
        private const val KEY_CITY = "saved_city"
        private const val KEY_TIMEZONE = "saved_timezone"
        private const val KEY_WEATHER_JSON = "weather_json"
        private const val KEY_LAST_FETCH_TIME = "last_fetch_time"
        private const val KEY_SAVE_SEARCH_ENABLED = "save_search_enabled"
        
        private const val KEY_LAST_TEMP = "last_temp"
        private const val KEY_LAST_CONDITION = "last_condition"
        private const val KEY_LAST_HUMIDITY = "last_humidity"

        const val DEFAULT_MUMBAI_NAME = "Mumbai"
        const val DEFAULT_MUMBAI_LAT = 19.0760
        const val DEFAULT_MUMBAI_LON = 72.8777
        const val DEFAULT_MUMBAI_TZ = "Asia/Kolkata"
    }
}
