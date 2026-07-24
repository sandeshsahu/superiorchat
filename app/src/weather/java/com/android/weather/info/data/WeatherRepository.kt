package com.android.weather.info.data

import android.util.Log
import com.android.weather.info.data.local.WeatherLocalStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    val localStorage: WeatherLocalStorage
) {
    private val weatherApi = NetworkModule.weatherApi
    private val geocodingApi = NetworkModule.geocodingApi
    private val ipApi = NetworkModule.ipApi
    
    private val gson = Gson()
    
    fun getCachedWeather(): WeatherResponse? {
        val json = localStorage.getWeatherJson() ?: return null
        return try {
            gson.fromJson(json, WeatherResponse::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getInitialLocation(forceRefresh: Boolean = false): Result<GeoResult> = withContext(Dispatchers.IO) {
        try {
            if (!forceRefresh && localStorage.hasSavedLocation()) {
                val savedLat = localStorage.getLat()
                val savedLon = localStorage.getLon()
                val savedCity = localStorage.getCityName()
                val savedTimezone = localStorage.getTimezone()

                if (savedCity.isNotEmpty()) {
                    return@withContext Result.success(
                        GeoResult(0, savedCity, "", "", savedLat, savedLon, savedTimezone)
                    )
                }
            }

            // Fresh installation or forced refresh: try IP-API
            try {
                val ipResponse = ipApi.getIpLocation()
                if (ipResponse.status == "success" && ipResponse.lat != null && ipResponse.lon != null && ipResponse.city != null) {
                    val tz = ipResponse.timezone ?: "auto"
                    localStorage.saveLocation(
                        lat = ipResponse.lat,
                        lon = ipResponse.lon,
                        cityName = ipResponse.city,
                        timezone = tz
                    )
                    return@withContext Result.success(
                        GeoResult(
                            id = 0,
                            name = ipResponse.city,
                            country = ipResponse.country ?: "",
                            admin1 = "",
                            latitude = ipResponse.lat,
                            longitude = ipResponse.lon,
                            timezone = tz
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("WeatherRepository", "IP location failed, defaulting to Mumbai", e)
            }

            // Fallback to Mumbai on fresh install if IP location fails or device is offline
            localStorage.saveLocation(
                lat = WeatherLocalStorage.DEFAULT_MUMBAI_LAT,
                lon = WeatherLocalStorage.DEFAULT_MUMBAI_LON,
                cityName = WeatherLocalStorage.DEFAULT_MUMBAI_NAME,
                timezone = WeatherLocalStorage.DEFAULT_MUMBAI_TZ
            )
            Result.success(
                GeoResult(
                    id = 0,
                    name = WeatherLocalStorage.DEFAULT_MUMBAI_NAME,
                    country = "India",
                    admin1 = "Maharashtra",
                    latitude = WeatherLocalStorage.DEFAULT_MUMBAI_LAT,
                    longitude = WeatherLocalStorage.DEFAULT_MUMBAI_LON,
                    timezone = WeatherLocalStorage.DEFAULT_MUMBAI_TZ
                )
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("WeatherRepository", "Error getting initial location", e)
            Result.failure(e)
        }
    }

    suspend fun getNearbyCities(lat: Double, lon: Double): Result<List<GeoResult>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkModule.countriesDevApi.getNearbyCities(lat, lon)
            
            if (response.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Map CountriesDev elements to GeoResult
            val geoResults = response.mapIndexed { index, element ->
                GeoResult(
                    id = index,
                    name = element.name,
                    country = element.countryCode,
                    admin1 = element.admin1Code,
                    latitude = element.latitude,
                    longitude = element.longitude,
                    timezone = element.timezone ?: "auto"
                )
            }.distinctBy { it.name }

            Result.success(geoResults)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("WeatherRepository", "Error getting nearby cities", e)
            Result.failure(e)
        }
    }

    suspend fun getWeather(lat: Double, lon: Double, timezone: String): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = weatherApi.getWeather(lat = lat, lon = lon, timezone = timezone)
            localStorage.saveWeatherJson(gson.toJson(response))
            Result.success(response)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("WeatherRepository", "Error fetching weather", e)
            Result.failure(e)
        }
    }

    suspend fun searchCity(query: String): Result<List<GeoResult>> = withContext(Dispatchers.IO) {
        try {
            val response = geocodingApi.searchCity(name = query)
            Result.success(response.results ?: emptyList())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("WeatherRepository", "Error searching city", e)
            Result.failure(e)
        }
    }
}
