package com.android.weather.info.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- API Interfaces ---

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode,precipitation_probability_max,uv_index_max,sunrise",
        @Query("hourly") hourly: String = "temperature_2m,weathercode,relativehumidity_2m,precipitation_probability,windspeed_10m,is_day",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 10
    ): WeatherResponse
}

interface GeocodingApiService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 15
    ): GeocodingResponse
}

interface IpLocationApiService {
    @GET("json/")
    suspend fun getIpLocation(): IpLocationResponse
}

interface CountriesDevApiService {
    @GET("cities/near")
    suspend fun getNearbyCities(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): List<CountriesDevCity>
}

// --- Retrofit Clients ---

object NetworkModule {
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/"
    private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"
    private const val IP_API_BASE_URL = "http://ip-api.com/" // ip-api free tier is HTTP only
    private const val COUNTRIES_DEV_BASE_URL = "https://countries.dev/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "WeatherAppInfo/1.0 (contact: info@weatherapp.com)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApi: WeatherApiService by lazy {
        buildRetrofit(WEATHER_BASE_URL).create(WeatherApiService::class.java)
    }

    val geocodingApi: GeocodingApiService by lazy {
        buildRetrofit(GEOCODING_BASE_URL).create(GeocodingApiService::class.java)
    }

    val ipApi: IpLocationApiService by lazy {
        buildRetrofit(IP_API_BASE_URL).create(IpLocationApiService::class.java)
    }

    val countriesDevApi: CountriesDevApiService by lazy {
        buildRetrofit(COUNTRIES_DEV_BASE_URL).create(CountriesDevApiService::class.java)
    }
}
