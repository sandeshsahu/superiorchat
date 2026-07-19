package com.mobile.superiorchat.ui

import com.mobile.superiorchat.ui.components.GlassCard
import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.BuildConfig


// ══════════════════════════════════════════════════════════
//  Settings Screen
// ══════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    isInternetConnected: Boolean,
    botToken: String,
    chatId: String,
    isAutoDownloadMediaEnabled: Boolean,
    isTileAccessEnabled: Boolean,
    isScreenSecurityEnabled: Boolean,
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onAutoDownloadMediaChange: (Boolean) -> Unit,
    onTileAccessChange: (Boolean) -> Unit,
    onScreenSecurityChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit = {}
) {
    val context = LocalContext.current

    var showAddManuallyDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var tempChatId by remember { mutableStateOf(chatId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    if (errorMessage != null) {
        com.mobile.superiorchat.ui.components.ErrorDialog(
            title = "Invalid Credentials",
            message = errorMessage!!,
            onDismiss = { errorMessage = null }
        )
    }

    if (showAddManuallyDialog) {
        if (botToken.isNotEmpty()) {
            com.mobile.superiorchat.ui.components.EditManuallyPopup(
                initialToken = botToken,
                initialChatId = chatId,
                onDismiss = { showAddManuallyDialog = false },
                onSave = { token, chat ->
                    onBotTokenChange(token)
                    onChatIdChange(chat)
                    onSave()
                    showAddManuallyDialog = false
                    Toast.makeText(context, "Credentials Saved", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            com.mobile.superiorchat.ui.components.AddManuallyPopup(
                onDismiss = { showAddManuallyDialog = false },
                onSave = { token, chat ->
                    onBotTokenChange(token)
                    onChatIdChange(chat)
                    onSave()
                    showAddManuallyDialog = false
                    Toast.makeText(context, "Credentials Saved", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showQrScanner) {
        com.mobile.superiorchat.ui.components.QrScanner(
            onDismiss = { showQrScanner = false },
            onSuccess = { token, chat ->
                onBotTokenChange(token)
                onChatIdChange(chat)
                onSave()
                showQrScanner = false
                Toast.makeText(context, "QR Configuration Applied", Toast.LENGTH_SHORT).show()
            },
            onShowGlobalDialog = onShowGlobalDialog
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // System Checks Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = "Build", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("System Checks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Success, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Token Access", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("Online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Success)
            }
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (isInternetConnected) Success else MaterialTheme.colorScheme.error, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Internet Connectivity", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(if (isInternetConnected) "Online" else "Offline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isInternetConnected) Success else MaterialTheme.colorScheme.error)
            }
        }

        // Bot Credentials Card
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bot Credentials", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val isConfigured = botToken.isNotEmpty()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.95f) { showAddManuallyDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isConfigured) "Edit Manually" else "Add Manually", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .glow(color = Primary, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 12.dp)
                            .background(Primary, RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.95f) { showQrScanner = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Scan QR Code", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // General Settings Card
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("General Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-Download Media", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                val scale by animateFloatAsState(
                    targetValue = if (isAutoDownloadMediaEnabled) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                Switch(
                    modifier = Modifier.scale(scale),
                    checked = isAutoDownloadMediaEnabled,
                    onCheckedChange = { onAutoDownloadMediaChange(it) },
                    thumbContent = if (isAutoDownloadMediaEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = TextPrimary
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = Background
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = Background.copy(alpha = 0.5f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Block Screenshots", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    var showSecurityInfo by remember { mutableStateOf(false) }
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = "Info", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.size(16.dp).clickable { showSecurityInfo = true }
                    )
                    
                    if (showSecurityInfo) {
                        com.mobile.superiorchat.ui.components.ErrorDialog(
                            title = "Screen Security",
                            message = "This prevents any app, screen recorder, or screen cast from capturing the chat. \n\nScreenshots will appear pure black.",
                            onDismiss = { showSecurityInfo = false }
                        )
                    }
                }
                
                val securityScale by animateFloatAsState(
                    targetValue = if (isScreenSecurityEnabled) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                Switch(
                    modifier = Modifier.scale(securityScale),
                    checked = isScreenSecurityEnabled,
                    onCheckedChange = { onScreenSecurityChange(it) },
                    thumbContent = if (isScreenSecurityEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = TextPrimary
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = Background
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = Background.copy(alpha = 0.5f)
                    )
                )
            }
        }


        if (BuildConfig.ENABLE_QS_TILE) {
            // App Accessibility Card
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Accessibility, contentDescription = "Accessibility", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("App Accessibility", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Access by Tile", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        var showAccessibilityInfo by remember { mutableStateOf(false) }
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = "Info", 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(16.dp).clickable { showAccessibilityInfo = true }
                        )
                        
                        if (showAccessibilityInfo) {
                            com.mobile.superiorchat.ui.components.ErrorDialog(
                                title = "Quick Settings Tile Access",
                                message = "Open notification panel, click on the pencil icon, find 'Carrier Sync' and add it.\n\nThen when you want to open chat:\n1. Enable\n2. Disable\n3. Enable\n4. Hold Tile to open chat app",
                                onDismiss = { showAccessibilityInfo = false }
                            )
                        }
                    }
                    
                    val scale by animateFloatAsState(
                        targetValue = if (isTileAccessEnabled) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )

                    Switch(
                        modifier = Modifier.scale(scale),
                        checked = isTileAccessEnabled,
                        onCheckedChange = { onTileAccessChange(it) },
                        thumbContent = if (isTileAccessEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = TextPrimary
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = Background
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = Primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = Background.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // About Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("About", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                InfoRow("App Name", "Superior Chat")
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                InfoRow("Author", "Sandesh")
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                InfoRow("Architecture", "Clean Architecture + MVVM")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Private Helpers
// ══════════════════════════════════════════════════════════

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(0.6f))
    }
}


