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
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
    tokenStatus: String,
    botToken: String,
    chatId: String,
    isTileAccessEnabled: Boolean,
    customAccessWord: String = "",
    webrtcBaseUrl: String,
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onTileAccessChange: (Boolean) -> Unit,
    onCustomAccessWordChange: (String) -> Unit = {},
    onWebrtcBaseUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit = {}
) {
    val context = LocalContext.current

    var showAddManuallyDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showDeveloperWarning by remember { mutableStateOf(false) }
    var showWebRtcConfigPopup by remember { mutableStateOf(false) }
    var tempChatId by remember { mutableStateOf(chatId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler(onShowGlobalDialog)
    
    if (errorMessage != null) {
        com.mobile.superiorchat.ui.components.popups.ErrorDialog(
            title = "Invalid Credentials",
            message = errorMessage!!,
            onDismiss = { errorMessage = null }
        )
    }

    if (showAddManuallyDialog) {
        com.mobile.superiorchat.ui.components.popups.CredentialsPopup(
            initialToken = botToken,
            initialChatId = chatId,
            onDismiss = { showAddManuallyDialog = false },
            onSave = { token, chat ->
                onBotTokenChange(token)
                onChatIdChange(chat)
                onSave()
                showAddManuallyDialog = false
                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Saved")
            }
        )
    }

    if (showQrScanner) {
        com.mobile.superiorchat.ui.components.QrScanner(
            onDismiss = { showQrScanner = false },
            onSuccess = { token, chat ->
                onBotTokenChange(token)
                onChatIdChange(chat)
                onSave()
                showQrScanner = false
                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "QR Configuration Applied")
            },
            onShowGlobalDialog = onShowGlobalDialog
        )
    }

    if (showDeveloperWarning) {
        com.mobile.superiorchat.ui.components.popups.ActionDialog(
            title = "Developer Setting",
            message = "This setting is strictly for *Developers*! Changing the *Server URL* can permanently *Break* the Calling feature. If you are not a developer, please *Cancel* this.",
            icon = Icons.Filled.Warning,
            iconTint = ErrorRed,
            confirmText = "I Understand",
            dismissText = "Cancel",
            onConfirm = {
                showWebRtcConfigPopup = true
            },
            onDismiss = { showDeveloperWarning = false }
        )
    }

    if (showWebRtcConfigPopup) {
        com.mobile.superiorchat.ui.components.popups.WebRtcConfigPopup(
            initialUrl = webrtcBaseUrl,
            onDismiss = { showWebRtcConfigPopup = false },
            onSave = { newUrl ->
                onWebrtcBaseUrlChange(newUrl)
                onSave()
                showWebRtcConfigPopup = false
                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "WebRTC URL Updated")
            }
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
                    Box(modifier = Modifier.size(8.dp).background(if (tokenStatus == "Online") Success else MaterialTheme.colorScheme.error, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Token Access", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(tokenStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (tokenStatus == "Online") Success else MaterialTheme.colorScheme.error)
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
                var showBotInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(4.dp).size(20.dp).clickable { showBotInfo = true }
                )
                
                if (showBotInfo) {
                    com.mobile.superiorchat.ui.components.popups.InfoDialog(
                        title = "Bot Credentials",
                        message = "You can manually enter your *Bot Token* and *Chat ID*, or securely import them by scanning a configuration *QR Code*.",
                        onDismiss = { showBotInfo = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val isConfigured = botToken.isNotEmpty()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bounceClick(scaleDown = 0.95f) { showAddManuallyDialog = true }
                            .background(SurfaceLevel2, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isConfigured) "Edit Manually" else "Add Manually", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                permissionHandler.requestCamera {
                                    showQrScanner = true
                                }
                            }
                            .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 12.dp)
                            .background(PrimaryLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Scan QR Code", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                            modifier = Modifier.padding(4.dp).size(16.dp).clickable { showAccessibilityInfo = true }
                        )
                        
                        if (showAccessibilityInfo) {
                            com.mobile.superiorchat.ui.components.popups.InfoDialog(
                                title = "Quick Settings Tile Access",
                                message = "Open notification panel, click on the pencil icon, find *Carrier Sync*' and add it.\n\nThen when you want to open chat:\n1. *Enable*\n2. *Disable*\n3. *Enable*\n4. *Hold Tile* to open chat app",
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
                        tint = PrimaryLight
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
                        colors = com.mobile.superiorchat.ui.components.luminaSwitchColors()
                    )
                }
            }
        }

        if (BuildConfig.FLAVOR == "weather") {
            // App Accessibility Card for Weather Flavor
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Accessibility, contentDescription = "Accessibility", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Set Custom Access Word", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    var showAccessInfo by remember { mutableStateOf(false) }
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = "Info", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.padding(4.dp).size(20.dp).clickable { showAccessInfo = true }
                    )
                    
                    if (showAccessInfo) {
                        com.mobile.superiorchat.ui.components.popups.InfoDialog(
                            title = "Custom Access Word",
                            message = "Set a secret phrase that you can type into the weather app's search bar to open Superior Chat. The default *superior chat* will always work as a fallback.",
                            onDismiss = { showAccessInfo = false }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                var tempWord by remember { mutableStateOf("") }
                val isValid = tempWord.trim().length >= 4
                var showWarning by remember { mutableStateOf(false) }
                var isSaved by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                LaunchedEffect(isSaved) {
                    if (isSaved) {
                        kotlinx.coroutines.delay(2000)
                        isSaved = false
                    }
                }
                
                if (showWarning) {
                    com.mobile.superiorchat.ui.components.popups.ActionDialog(
                        title = "Warning",
                        message = "Are you sure you want to set your access word to *${tempWord.trim()}*? If you forget this word, you can always use the default *superior chat* fallback to regain access.",
                        icon = Icons.Default.Warning,
                        iconTint = PrimaryLight,
                        confirmText = "Save",
                        onConfirm = {
                            onCustomAccessWordChange(tempWord.trim())
                            tempWord = ""
                            
                            // Trigger save animation
                            isSaved = true
                        },
                        onDismiss = { showWarning = false }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempWord,
                        onValueChange = { tempWord = it },
                        placeholder = { Text("e.g. open door", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    if (customAccessWord.isNotEmpty()) {
                        Text(
                            text = "Current saved word: $customAccessWord",
                            color = PrimaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                if (isValid && !isSaved) {
                                    showWarning = true
                                }
                            }
                            .glow(color = if (isSaved) Success else if (isValid) PrimaryLight else Color.Transparent, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 12.dp)
                            .background(if (isSaved) Success else if (isValid) PrimaryLight else SurfaceLevel2, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = isSaved,
                            transitionSpec = {
                                (scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith 
                                (scaleOut(targetScale = 0.8f, animationSpec = tween(150)) + fadeOut())
                            },
                            label = "save_animation"
                        ) { saved ->
                            if (saved) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Saved!", 
                                        color = MaterialTheme.colorScheme.onPrimaryContainer, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Save Custom Word", 
                                    color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary, 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // WebRTC Configuration Card
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Call Configuration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                var showWebRtcInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(4.dp).size(20.dp).clickable { showWebRtcInfo = true }
                )
                
                if (showWebRtcInfo) {
                    com.mobile.superiorchat.ui.components.popups.InfoDialog(
                        title = "Call Configuration",
                        message = "You can configure your custom *WebRTC Server* URL for voice calls, or reset it to the default server if you experience connection issues.\n\nCheck developer's *Github Page* for more information",
                        onDismiss = { showWebRtcInfo = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.95f) { showDeveloperWarning = true }
                        .background(SurfaceLevel2, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Change Server", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(scaleDown = 0.95f) {
                            onWebrtcBaseUrlChange(com.mobile.superiorchat.data.Prefs.DEFAULT_WEBRTC_URL)
                            onSave()
                            com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "WebRTC URL Reset")
                        }
                        .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 12.dp)
                        .background(PrimaryLight, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Reset to Default", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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


