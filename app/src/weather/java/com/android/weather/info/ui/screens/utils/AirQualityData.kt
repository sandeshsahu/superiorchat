package com.android.weather.info.ui.screens.utils
import com.mobile.superiorchat.R


import androidx.annotation.DrawableRes

data class AirQualityItem(
    @DrawableRes val icon: Int,
    val title: String,
    val value: String
)

val AirQualityData = listOf(
    AirQualityItem(
        title = "Real Feel",
        value = "23.8",
        icon = R.drawable.ic_real_feel
    ),
    AirQualityItem(
        title = "Wind",
        value = "9km/h",
        icon = R.drawable.ic_wind_qality,
    ),
    AirQualityItem(
        title = "Humidity",
        value = "60%",
        icon = R.drawable.ic_so2
    ),
    AirQualityItem(
        title = "Sunrise",
        value = "6:30 AM",
        icon = R.drawable.ic_o3
    )
)