package com.mobile.superiorchat.ui

import com.mobile.superiorchat.ui.components.GlassCard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.BuildConfig

// ══════════════════════════════════════════════════════════
//  App Settings Page
// ══════════════════════════════════════════════════════════

@Composable
fun AppSettingsPage(
    isInternetConnected: Boolean,
    tokenStatus: String,
    hasCredentials: Boolean,
    botToken: String,
    chatId: String,
    isTileAccessEnabled: Boolean,
    customAccessWord: String = "",
    webrtcBaseUrl: String,
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onTileAccessChange: (Boolean) -> Unit,
    onCustomAccessWordChange: (String) -> Unit = {},
    onAutoDownloadMediaChange: (Boolean) -> Unit,
    onScreenSecurityChange: (Boolean) -> Unit,
    onNewMessageNotificationChange: (Boolean) -> Unit,
    onWebrtcBaseUrlChange: (String) -> Unit,
    isAppLockEnabled: Boolean,
    isFakeCrashEnabled: Boolean,
    onAppLockChange: (Boolean, String) -> Unit,
    onFakeCrashChange: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    verifyPin: (String) -> Boolean,
    onSave: () -> Unit,
    onClearCredentials: () -> Unit,
    onClearChat: (Boolean) -> Unit,
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit = {}
) {
    val context = LocalContext.current

    var showAddManuallyDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showDeveloperWarning by remember { mutableStateOf(false) }
    var showWebRtcConfigPopup by remember { mutableStateOf(false) }
    var isResettingWebRtc by remember { mutableStateOf(false) }
    var showAllServersUnavailable by remember { mutableStateOf(false) }
    var showNetworkError by remember { mutableStateOf(false) }
    var showDangerZone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler(onShowGlobalDialog)

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var showChangePinSetupDialog by remember { mutableStateOf(false) }
    var verifyAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinErrorMsg by remember { mutableStateOf("") }
    val appName = androidx.compose.ui.res.stringResource(id = com.mobile.superiorchat.R.string.app_name)

    if (showPinSetupDialog) {
        com.mobile.superiorchat.ui.components.popups.PinSetupPopup(
            onDismiss = { showPinSetupDialog = false },
            onSave = { pin ->
                onAppLockChange(true, pin)
                showPinSetupDialog = false
            }
        )
    }

    if (showChangePinSetupDialog) {
        com.mobile.superiorchat.ui.components.popups.PinSetupPopup(
            onDismiss = { showChangePinSetupDialog = false },
            onSave = { pin ->
                onChangePin(pin)
                showChangePinSetupDialog = false
                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "PIN Changed")
            }
        )
    }

    if (showPinVerifyDialog) {
        com.mobile.superiorchat.ui.components.popups.PinVerifyPopup(
            errorMsg = pinErrorMsg,
            onDismiss = { 
                showPinVerifyDialog = false 
                pinErrorMsg = ""
            },
            onVerify = { pin ->
                if (verifyPin(pin)) {
                    verifyAction?.invoke()
                    showPinVerifyDialog = false
                    pinErrorMsg = ""
                } else {
                    pinErrorMsg = "Incorrect PIN"
                }
            }
        )
    }
    
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
            onSuccess = { data ->
                onBotTokenChange(data.token)
                onChatIdChange(data.chatId)
                data.autoDownloadMedia?.let { onAutoDownloadMediaChange(it) }
                data.newMessageNotification?.let { onNewMessageNotificationChange(it) }
                data.screenSecurity?.let { onScreenSecurityChange(it) }
                data.callServer?.let { 
                    onWebrtcBaseUrlChange(it)
                }
                
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

    if (showAllServersUnavailable) {
        com.mobile.superiorchat.ui.components.popups.ActionDialog(
            title = "Servers Unavailable",
            message = "All working servers are currently *Unavailable*.\nPlease contact the *Developer* or check the GitHub page to learn how to deploy your own static *PeerJS signaling server*.",
            icon = Icons.Filled.Warning,
            iconTint = ErrorRed,
            confirmText = "Okay",
            dismissText = "",
            onConfirm = { showAllServersUnavailable = false },
            onDismiss = { showAllServersUnavailable = false },
            isLoading = false,
            isSuccess = false,
            autoDismiss = true
        )
    }

    if (showNetworkError) {
        com.mobile.superiorchat.ui.components.popups.ActionDialog(
            title = "No Internet Connection",
            message = "Please check your network connection and try again.",
            icon = Icons.Filled.Warning,
            iconTint = ErrorRed,
            confirmText = "Okay",
            dismissText = "",
            onConfirm = { showNetworkError = false },
            onDismiss = { showNetworkError = false },
            isLoading = false,
            isSuccess = false,
            autoDismiss = true
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
            .padding(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1st: Security Settings Section ────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Security Settings",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(12.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        }

        // App Lock Card
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Lock", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                var showAppLockInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = PrimaryLight, 
                    modifier = Modifier.size(20.dp).clickable { showAppLockInfo = true }
                )
                if (showAppLockInfo) {
                    com.mobile.superiorchat.ui.components.popups.InfoDialog(
                        title = "App Lock",
                        message = "App Lock secures your chats by requiring a PIN code every time you open the app or return from the background.",
                        onDismiss = { showAppLockInfo = false }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Require PIN to open",
                subtitle = "Lock app when minimized",
                icon = Icons.Default.VpnKey,
                iconTint = PrimaryLight,
                isChecked = isAppLockEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        showPinSetupDialog = true
                    } else {
                        verifyAction = { onAppLockChange(false, "") }
                        showPinVerifyDialog = true
                    }
                }
            )

            if (isAppLockEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsActionRow(
                    title = "Change PIN",
                    subtitle = "Update your access code",
                    icon = Icons.Filled.Password,
                    onClick = {
                        verifyAction = { showChangePinSetupDialog = true }
                        showPinVerifyDialog = true
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                var showSafeguardInfoDialog by remember { mutableStateOf(false) }
                SettingsActionRow(
                    title = "Safeguard Active (Code: 1234)",
                    subtitle = "Tap to learn how emergency fake unlock works",
                    icon = Icons.Filled.Security,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = PrimaryLight,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isGlow = true,
                    onClick = { showSafeguardInfoDialog = true }
                )

                if (showSafeguardInfoDialog) {
                    com.mobile.superiorchat.ui.components.popups.ActionDialog(
                        title = "Emergency Safeguard",
                        message = "If someone forces you to open the app, type *1234* on the lock screen instead of your real PIN.\n\nThe app will immediately *Lock* itself and switch to a *fake screen* without revealing your *Chats*.",
                        icon = Icons.Filled.Security,
                        iconTint = PrimaryLight,
                        confirmText = "Got it",
                        dismissText = "",
                        onConfirm = { showSafeguardInfoDialog = false },
                        onDismiss = { showSafeguardInfoDialog = false }
                    )
                }
            }
        }

        // Removed weather check for Fake Crash
            // Fake Crash Card
            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Fake Crash Decoy", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    var showFakeCrashInfo by remember { mutableStateOf(false) }
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = "Info", 
                        tint = PrimaryLight, 
                        modifier = Modifier.size(20.dp).clickable { showFakeCrashInfo = true }
                    )
                    if (showFakeCrashInfo) {
                        com.mobile.superiorchat.ui.components.popups.InfoDialog(
                            title = "Fake Crash Decoy",
                            message = "When enabled, an authentic-looking system fake crash dialog will appear when opening app.\n\nTo open the Chat, you must **Hold** the Word \n'**$appName**' <-- for **2 seconds**.",
                            onDismiss = { showFakeCrashInfo = false }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                var showFakeCrashWarning by remember { mutableStateOf(false) }
                if (showFakeCrashWarning) {
                    com.mobile.superiorchat.ui.components.popups.ActionDialog(
                        title = "Enable Fake Crash?",
                        message = "You are about to enable Fake Crash. This will display fake **Crash Dialog on Startup**.\n\nTo open the Chat, you must **Hold** the Word \n'**$appName**' <-- for **2 seconds**. Do not forget this!",
                        icon = Icons.Default.Warning,
                        iconTint = ErrorRed,
                        confirmText = "Enable",
                        onConfirm = { 
                            onFakeCrashChange(true)
                            showFakeCrashWarning = false
                        },
                        onDismiss = { showFakeCrashWarning = false }
                    )
                }

                SettingsSwitchRow(
                    title = "Enable Fake Crash",
                    subtitle = "Shows fake crash dialog on startup",
                    icon = Icons.Default.Warning,
                    iconTint = if (isFakeCrashEnabled) PrimaryLight else ErrorRed,
                    isChecked = isFakeCrashEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            showFakeCrashWarning = true
                        } else {
                            onFakeCrashChange(false)
                        }
                    }
                )
            }

        // ── 2nd: Flavor Specific Section ────────────────────────────
        val hasFlavorSettings = BuildConfig.ENABLE_QS_TILE || BuildConfig.FLAVOR == "weather"
        if (hasFlavorSettings) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Flavor Specific",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            }

            if (BuildConfig.ENABLE_QS_TILE) {
                // App Accessibility Card
                SettingsCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Accessibility, contentDescription = "Accessibility", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("App Accessibility", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    var showAccessibilityInfo by remember { mutableStateOf(false) }
                    var showTileDisableWarning by remember { mutableStateOf(false) }
                    
                    if (showAccessibilityInfo) {
                        com.mobile.superiorchat.ui.components.popups.InfoDialog(
                            title = "Quick Settings Tile Access",
                            message = "Open notification panel, click on the pencil icon, find *Carrier Sync*' and add it.\n\nThen when you want to open chat:\n1. *Enable*\n2. *Disable*\n3. *Enable*\n4. *Hold Tile* to open chat app",
                            onDismiss = { showAccessibilityInfo = false }
                        )
                    }

                    if (showTileDisableWarning) {
                        com.mobile.superiorchat.ui.components.popups.ActionDialog(
                            title = "Disable Tile Access",
                            message = "If you disable this, you will no longer be able to *Open The App* using the *Notification Tile*.\nIf access by dialer fails, you may be *Completely Locked Out* of the app.\nAre you sure you want to *Proceed*?",
                            icon = Icons.Filled.Warning,
                            iconTint = ErrorRed,
                            confirmText = "Disable",
                            onConfirm = {
                                onTileAccessChange(false)
                                showTileDisableWarning = false
                            },
                            onDismiss = { showTileDisableWarning = false }
                        )
                    }

                    SettingsSwitchRow(
                        title = "Access by Tile",
                        subtitle = "Use Quick Settings to open",
                        icon = Icons.Default.SettingsInputAntenna,
                        iconTint = PrimaryLight,
                        isChecked = isTileAccessEnabled,
                        onCheckedChange = { isChecked ->
                            if (!isChecked) {
                                showTileDisableWarning = true
                            } else {
                                onTileAccessChange(true)
                            }
                        },
                        onInfoClick = { showAccessibilityInfo = true }
                    )
                }
            }

            if (BuildConfig.FLAVOR == "weather") {
                // Custom Access Word for Weather Flavor
                SettingsCard {
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
                                message = "Set a secret phrase that you can type into the weather app's search bar to open Superior Chat. The default *Superior Chat* will always work as a fallback.",
                                onDismiss = { showAccessInfo = false }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    var tempWord by remember { mutableStateOf("") }
                    val isValid = tempWord.trim().length >= 4
                    var showWarning by remember { mutableStateOf(false) }
                    var isSaved by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(isSaved) {
                        if (isSaved) {
                            kotlinx.coroutines.delay(2000)
                            isSaved = false
                        }
                    }
                    
                    if (showWarning) {
                        com.mobile.superiorchat.ui.components.popups.ActionDialog(
                            title = "Warning",
                            message = "Are you sure you want to set your access word to *${tempWord.trim()}*? If you forget this word, you can always use the default *Superior Chat* fallback to regain access.",
                            icon = Icons.Default.Warning,
                            iconTint = PrimaryLight,
                            confirmText = "Save",
                            onConfirm = {
                                onCustomAccessWordChange(tempWord.trim())
                                tempWord = ""
                                isSaved = true
                            },
                            onDismiss = { showWarning = false }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

                        SettingsActionRow(
                            title = if (isSaved) "Saved!" else "Save Custom Word",
                            subtitle = "Apply new phrase",
                            icon = if (isSaved) Icons.Filled.Check else Icons.Filled.Save,
                            iconTint = if (isSaved) Color.White else if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                            background = if (isSaved) Success else if (isValid) PrimaryLight else SurfaceLevel2,
                            contentColor = if (isSaved) Color.White else if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                            isGlow = isValid || isSaved,
                            onClick = {
                                if (isValid && !isSaved) {
                                    showWarning = true
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── 3rd: Developer Settings Section ────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Developer Settings",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(12.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        }

        // Bot Credentials Card
        SettingsCard {
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
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isConfigured = botToken.isNotEmpty()
                SettingsActionRow(
                    title = if (isConfigured) "Edit Manually" else "Add Manually",
                    subtitle = "Type credentials by hand",
                    icon = if (isConfigured) Icons.Filled.Edit else Icons.Filled.Add,
                    iconTint = if (isConfigured) TextSecondary else PrimaryLight,
                    onClick = { showAddManuallyDialog = true }
                )

                SettingsActionRow(
                    title = "Scan QR Code",
                    subtitle = "Fastest & recommended",
                    icon = Icons.Filled.QrCodeScanner,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = PrimaryLight,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isGlow = true,
                    onClick = { 
                        permissionHandler.requestCamera {
                            showQrScanner = true
                        }
                    }
                )
            }
        }

        // Call Configuration Card
        SettingsCard {
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
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsActionRow(
                    title = "Change Server",
                    subtitle = "Configure custom WebRTC URL",
                    icon = Icons.Filled.Dns,
                    iconTint = PrimaryLight,
                    onClick = { showDeveloperWarning = true }
                )

                SettingsActionRow(
                    title = if (isResettingWebRtc) "Resetting..." else "Reset to Default",
                    subtitle = "Restore default connection",
                    icon = Icons.Filled.Restore,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = PrimaryLight,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isGlow = true,
                    onClick = {
                        if (isResettingWebRtc) return@SettingsActionRow
                        
                        scope.launch {
                            isResettingWebRtc = true
                            val fallbackUrls = context.resources.getStringArray(com.mobile.superiorchat.R.array.webrtc_fallback_urls).toList().shuffled()
                            val result = com.mobile.superiorchat.core.call.CallManager.findWorkingFallbackUrl(context, fallbackUrls)
                            
                            if (result.url != null) {
                                onWebrtcBaseUrlChange(result.url)
                                onSave()
                                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "WebRTC URL Reset")
                            } else if (result.networkFailed) {
                                showNetworkError = true
                            } else {
                                showAllServersUnavailable = true
                            }
                            isResettingWebRtc = false
                        }
                    }
                )
            }
        }

        // ── 4th: Danger Zone ────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.25f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Danger Zone",
                    color = ErrorRed.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.25f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLevel1, RoundedCornerShape(18.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ErrorRed.copy(alpha = 0.07f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    SettingsActionRow(
                        title = "Danger Zone",
                        subtitle = "Sensitive and destructive actions",
                        icon = Icons.Filled.Warning,
                        iconTint = ErrorRed,
                        contentColor = ErrorRed,
                        onClick = { showDangerZone = true }
                    )
                }
            }
        }
    }

    if (showDangerZone) {
        DangerZoneSheet(
            hasCredentials = hasCredentials,
            onDismiss = { showDangerZone = false },
            onClearCredentials = onClearCredentials,
            onClearChat = onClearChat
        )
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

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    background: androidx.compose.ui.graphics.Color = SurfaceLevel2,
    contentColor: androidx.compose.ui.graphics.Color = TextPrimary,
    isGlow: Boolean = false,
    onClick: () -> Unit
) {
    var modifier = Modifier
        .fillMaxWidth()
        .bounceClick(scaleDown = 0.95f) { onClick() }
        
    if (isGlow) {
        modifier = modifier.glow(color = background, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 14.dp)
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = contentColor, fontWeight = FontWeight.Medium)
                Text(subtitle, color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "switch_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    if (onInfoClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = TextSecondary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                                .clickable { onInfoClick() }
                        )
                    }
                }
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        
        Switch(
            modifier = Modifier.scale(scale),
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (isChecked) {
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

// ══════════════════════════════════════════════════════════
//  Danger Zone Sheet
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DangerZoneSheet(
    hasCredentials: Boolean,
    onDismiss: () -> Unit,
    onClearCredentials: () -> Unit,
    onClearChat: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showClearConfirm by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceLevel2)
                        .bounceClick {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Danger Zone", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sensitive actions", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasCredentials) {
                // Clear Chat Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ErrorRed.copy(alpha = 0.07f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    DangerRow(
                        icon = Icons.Filled.Delete,
                        title = "Clear Chat",
                        subtitle = "Permanently delete all messages",
                        onClick = { showClearChatConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Clear Credentials Button
            if (!hasCredentials) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.NoAccounts, null, tint = ErrorRed.copy(0.5f), modifier = Modifier.size(16.dp))
                    Text("Credentials Empty", color = ErrorRed.copy(0.5f), fontSize = 13.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ErrorRed.copy(alpha = 0.07f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    DangerRow(
                        icon = Icons.Filled.NoAccounts,
                        title = "Clear Credentials",
                        subtitle = "Disconnect the bot and stop chatting",
                        onClick = { showClearConfirm = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uninstall App
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(alpha = 0.07f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                DangerRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "Uninstall App",
                    subtitle = "Remove this application entirely",
                    onClick = { showUninstallConfirm = true }
                )
            }
        }
    }

    if (showClearConfirm) {
        com.mobile.superiorchat.ui.components.popups.ActionDialog(
            title = "Clear Credentials",
            message = "This will disconnect the bot and stop the end-to-end chat. *Both Users* will lose access\nto the current *Chat Session*. Do you want to proceed?",
            icon = Icons.Filled.NoAccounts,
            iconTint = ErrorRed,
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                onClearCredentials()
                showClearConfirm = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showClearChatConfirm) {
        com.mobile.superiorchat.ui.components.popups.ClearChatWarningDialog(
            onDismiss = { showClearChatConfirm = false },
            onConfirmClear = { deleteMedia ->
                onClearChat(deleteMedia)
                showClearChatConfirm = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }

    if (showUninstallConfirm) {
        com.mobile.superiorchat.ui.components.popups.ActionDialog(
            title = "Uninstall App",
            message = "This will *permanently remove* the application from your device. Do you want to proceed?",
            icon = Icons.Filled.DeleteForever,
            iconTint = ErrorRed,
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_DELETE, android.net.Uri.parse("package:${context.packageName}")))
                showUninstallConfirm = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onDismiss = { showUninstallConfirm = false }
        )
    }
}

@Composable
private fun DangerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ErrorRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = ErrorRed.copy(0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceLevel1)
            .padding(16.dp),
        content = content
    )
}
