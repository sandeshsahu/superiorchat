package com.mobile.superiorchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.BuildConfig
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick

// ══════════════════════════════════════════════════════════
//  App Information Screen
// ══════════════════════════════════════════════════════════

@Composable
fun AppScreenPage(
    isInternetConnected: Boolean,
    tokenStatus: String,
    onNavigate: (NavScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── System Checks Section ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLevel1, RoundedCornerShape(20.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("System Checks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppScreenStatusRow(
                    title = "Token Access",
                    subtitle = tokenStatus,
                    icon = Icons.Filled.Key,
                    isOk = tokenStatus == "Online"
                )
                AppScreenStatusRow(
                    title = "Internet Connectivity",
                    subtitle = if (isInternetConnected) "Online" else "Offline",
                    icon = Icons.Filled.Wifi,
                    isOk = isInternetConnected
                )
            }
        }

        // ── Shortcuts Section ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLevel1, RoundedCornerShape(20.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Shortcuts", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppScreenActionRow(
                    title = "Permissions",
                    subtitle = "Manage app access",
                    icon = Icons.Filled.Lock,
                    iconTint = TextSecondary,
                    onClick = { onNavigate(NavScreen.Permissions) }
                )
                AppScreenActionRow(
                    title = "App Logs",
                    subtitle = "View system logs",
                    icon = Icons.Filled.Terminal,
                    iconTint = TextSecondary,
                    onClick = { onNavigate(NavScreen.Logs) }
                )
                AppScreenActionRow(
                    title = "App Settings",
                    subtitle = "Configure application",
                    icon = Icons.Filled.Settings,
                    iconTint = TextSecondary,
                    onClick = { onNavigate(NavScreen.AppSettings) }
                )
            }
        }

        // ── About Section ──────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLevel1, RoundedCornerShape(20.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("About", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLevel2, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AboutInfoRow("App Name", "Superior Chat")
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                AboutInfoRow("Author", "@sandeshsahu")
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                AboutInfoRow("Architecture", "Clean + MVVM")
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                AboutInfoRow("Version", BuildConfig.VERSION_NAME)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Private Helpers
// ══════════════════════════════════════════════════════════

@Composable
private fun AppScreenStatusRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isOk: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isOk) Success else MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, color = if (isOk) Success else MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Box(modifier = Modifier.size(8.dp).background(if (isOk) Success else MaterialTheme.colorScheme.error, CircleShape))
    }
}

@Composable
private fun AppScreenActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.95f) { onClick() }
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextPrimary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(0.6f))
    }
}
