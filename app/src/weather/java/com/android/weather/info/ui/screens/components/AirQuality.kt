package com.android.weather.info.ui.screens.components

import com.mobile.superiorchat.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.weather.info.ui.screens.utils.AirQualityData
import com.android.weather.info.ui.screens.utils.AirQualityItem
import com.android.weather.info.ui.theme.ColorAirQualityIconTitle
import com.android.weather.info.ui.theme.ColorSurface
import com.android.weather.info.ui.theme.ColorTextPrimary
import com.android.weather.info.ui.theme.ColorTextPrimaryVariant
import com.android.weather.info.data.WeatherDisplayData

@Composable
fun AirQuality (
    data: WeatherDisplayData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = ColorSurface
    ) {
        Column(
            modifier = Modifier
                .padding(
                    vertical = 18.dp,
                    horizontal = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AirQualityHeader()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AirQualityInfo(
                    data = AirQualityItem(R.drawable.ic_wind_qality, "Wind", data.windSpeed)
                )
                AirQualityInfo(
                    data = AirQualityItem(R.drawable.ic_so2, "Humidity", data.humidity)
                )
                AirQualityInfo(
                    data = AirQualityItem(R.drawable.ic_o3, "Sunrise", data.sunrise)
                )
            }
        }
    }
}

@Composable
fun AirQualityHeader (
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_air_quality_header),
                contentDescription = null,
                tint = ColorAirQualityIconTitle,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Air Quality",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                )
            )
        }
    }
}




@Composable
fun AirQualityInfo (
    modifier: Modifier = Modifier,
    data: AirQualityItem
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(data.icon),
            contentDescription = null,
            tint = ColorAirQualityIconTitle,
            modifier = Modifier
                .size(24.dp)
        )
        Column (
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTextPrimaryVariant
            )
            Text(
                text = data.value,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTextPrimary
            )
        }
    }
}