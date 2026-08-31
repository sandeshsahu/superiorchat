package com.mobile.superiorchat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.BuildConfig
import com.mobile.superiorchat.R
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
                    iconTint = PrimaryLight,
                    onClick = { onNavigate(NavScreen.Permissions) }
                )
                AppScreenActionRow(
                    title = "Call History",
                    subtitle = "View past calls",
                    icon = Icons.Filled.History,
                    iconTint = PrimaryLight,
                    onClick = { onNavigate(NavScreen.CallHistory) }
                )
                AppScreenActionRow(
                    title = "App Logs",
                    subtitle = "View system logs",
                    icon = Icons.Filled.Terminal,
                    iconTint = PrimaryLight,
                    onClick = { onNavigate(NavScreen.Logs) }
                )
                AppScreenActionRow(
                    title = "App Settings",
                    subtitle = "Configure application",
                    icon = Icons.Filled.Settings,
                    iconTint = PrimaryLight,
                    onClick = { onNavigate(NavScreen.AppSettings) }
                )
                AppScreenActionRow(
                    title = "Admin Settings",
                    subtitle = "Advanced configuration",
                    icon = Icons.Filled.AdminPanelSettings,
                    iconTint = PrimaryLight,
                    onClick = { onNavigate(NavScreen.AdminSettings) }
                )
            }
        }

        // ── About Section ──────────────────────────────
        val context = LocalContext.current
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
                AboutLinksRow(
                    onGithubClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sandeshsahu/")))
                    },
                    onGitlabClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/sandeshsahu")))
                    },
                    onLinkedinClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/sandesh-sahu/")))
                    }
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AboutLinksRow(
    onGithubClick: () -> Unit,
    onGitlabClick: () -> Unit,
    onLinkedinClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Links", color = TextSecondary, fontSize = 14.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "GitHub",
                tint = TextPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onGithubClick)
            )
            Icon(
                painter = painterResource(R.drawable.ic_gitlab),
                contentDescription = "GitLab",
                tint = TextPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onGitlabClick)
            )
            Icon(
                painter = painterResource(R.drawable.ic_linkedin),
                contentDescription = "LinkedIn",
                tint = TextPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLinkedinClick)
            )
        }
    }
}
