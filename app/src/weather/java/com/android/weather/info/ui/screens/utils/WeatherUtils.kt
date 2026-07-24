package com.android.weather.info.ui.screens.utils
import com.mobile.superiorchat.R

import androidx.compose.ui.graphics.Color

fun Color.Companion.fromHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}

fun getWeatherIconForCode(code: Int, isDay: Boolean = true): Int = when(code) {
    0          -> if (isDay) R.drawable.img_sun else R.drawable.img_moon_stars
    1, 2, 3    -> if (isDay) R.drawable.img_clouds else R.drawable.img_moon_stars
    45, 48     -> R.drawable.img_cloudy
    51,53,55,
    61,63,65,
    66,67,
    80,81,82   -> R.drawable.img_rain
    71,73,75,
    77,85,86   -> R.drawable.img_moon_stars
    95,96,99   -> R.drawable.img_thunder
    else       -> R.drawable.img_clouds
}

fun getWeatherDescriptionForCode(code: Int): String = when(code) {
    0          -> "Clear Sky"
    1, 2       -> "Partly Cloudy"
    3          -> "Overcast"
    45, 48     -> "Foggy"
    51,53,55   -> "Drizzle"
    61,63,65   -> "Rain"
    80,81,82   -> "Showers"
    95,96,99   -> "Thunderstorm"
    71,73,75   -> "Snow"
    else       -> "Cloudy"
}
