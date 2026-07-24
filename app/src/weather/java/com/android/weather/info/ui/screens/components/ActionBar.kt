package com.android.weather.info.ui.screens.components

import com.mobile.superiorchat.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.weather.info.ui.theme.ColorGradient1
import com.android.weather.info.ui.theme.ColorGradient2
import com.android.weather.info.ui.theme.ColorGradient3
import com.android.weather.info.ui.theme.ColorImageShadow
import com.android.weather.info.ui.theme.ColorSurface
import com.android.weather.info.ui.theme.ColorTextPrimary
import com.android.weather.info.ui.theme.ColorTextPrimaryVariant
import com.android.weather.info.ui.theme.ColorTextSecondaryVariant


@Composable
fun ActionBar (
    cityName: String,
    isSaveSearchEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSaveSearchToggle: (Boolean) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        SaveSearchToggle(
            isEnabled = isSaveSearchEnabled,
            onToggle = onSaveSearchToggle,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        LocationInfo(
            cityName = cityName,
            modifier = Modifier.align(Alignment.Center)
        )
        
        SearchButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable { onSearchClick() }
        )
    }
}

@Composable
fun SaveSearchToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorTextPrimary,
                checkedTrackColor = ColorTextPrimaryVariant.copy(alpha = 0.5f),
                uncheckedThumbColor = ColorSurface,
                uncheckedTrackColor = ColorTextPrimaryVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.height(24.dp)
        )
        Text(
            text = "Save Search",
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextPrimaryVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SearchButton (
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .border(
                width = 1.5.dp,
                color = ColorSurface,
                shape = CircleShape
            )
            .customShadow(
                color = Color.Black,
                alpha = 0.1f,
                shadowRadius = 12.dp,
                borderRadius = 48.dp,
                offsetY = 6.dp
            )
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = ColorTextPrimaryVariant,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ColorSurface)
                .padding(8.dp)
        )
    }
}

@Composable
private fun LocationInfo(
    cityName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_location_pin),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(18.dp)
            )
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleLarge,
                color = ColorTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
