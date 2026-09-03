package com.mobile.superiorchat.ui

import com.mobile.superiorchat.ui.components.GlassCard
import com.mobile.superiorchat.ui.components.popups.*
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
import androidx.compose.ui.graphics.graphicsLayer
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
    customDialerCode: String = "",
    onCustomDialerCodeChange: (String) -> Unit = {},
    isPeerLinkEnabled: Boolean = false,
    onPeerLinkChange: (Boolean) -> Unit = {},
    isAdminModeEnabled: Boolean = false,
    peerLinkPartnerBotUsername: String = "",
    onPeerLinkPartnerBotUsernameChange: (String) -> Unit = {},
    webrtcBaseUrl: String,
    appTheme: com.mobile.superiorchat.theme.AppTheme,
    onAppThemeChange: (com.mobile.superiorchat.theme.AppTheme) -> Unit,
    isAnimeCharacterEnabled: Boolean = true,
    onAnimeCharacterChange: (Boolean) -> Unit = {},
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onTileAccessChange: (Boolean) -> Unit,
    onCustomAccessWordChange: (String) -> Unit = {},
    onAutoDownloadMediaChange: (Boolean) -> Unit,
    onScreenSecurityChange: (Boolean) -> Unit,
    onNewMessageNotificationChange: (Boolean) -> Unit,
    onCallNotificationsChange: (Boolean) -> Unit = {},
    onQrConfigApplied: ((com.mobile.superiorchat.utils.QrConfigData) -> Unit)? = null,
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
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit = {},
    onNavigateToAdmin: () -> Unit = {}
) {
    val context = LocalContext.current

    var showAddManuallyDialog by remember { mutableStateOf(false) }
    var showManualCredentialsWarning by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showDeveloperWarning by remember { mutableStateOf(false) }
    var showWebRtcConfigPopup by remember { mutableStateOf(false) }
    var isResettingWebRtc by remember { mutableStateOf(false) }
    var showAllServersUnavailable by remember { mutableStateOf(false) }
    var showNetworkError by remember { mutableStateOf(false) }
    var showDangerZone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdminModeActiveWarning by remember { mutableStateOf(false) }
    var showAdminQrWarning by remember { mutableStateOf(false) }
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
        SettingsInvalidCredentialsDialog(
            errorMessage = errorMessage!!,
            onDismiss = { errorMessage = null }
        )
    }

    if (showManualCredentialsWarning) {
        SettingsManualCredentialsWarningDialog(
            isEditing = botToken.isNotEmpty(),
            onConfirm = {
                showManualCredentialsWarning = false
                showAddManuallyDialog = true
            },
            onDismiss = { showManualCredentialsWarning = false }
        )
    }

    if (showAddManuallyDialog) {
        com.mobile.superiorchat.ui.components.popups.CredentialsPopup(
            initialToken = botToken,
            initialChatId = chatId,
            initialPartnerUsername = peerLinkPartnerBotUsername,
            isPeerLinkEnabled = isPeerLinkEnabled,
            onDismiss = { showAddManuallyDialog = false },
            onSave = { token, chat, partner ->
                val isGroup = chat.trim().startsWith("-")
                val hasPartnerBot = partner.trim().isNotBlank()
                onBotTokenChange(token)
                onChatIdChange(chat)
                if (!isGroup || !hasPartnerBot) {
                    onPeerLinkPartnerBotUsernameChange("")
                    if (isPeerLinkEnabled) {
                        onPeerLinkChange(false)
                    }
                } else {
                    onPeerLinkPartnerBotUsernameChange(partner)
                    if (!isPeerLinkEnabled) {
                        onPeerLinkChange(true)
                    }
                }
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
                showQrScanner = false
                if (data.role == "ADMIN" || data.isAdminModeEnabled == true) {
                    showAdminQrWarning = true
                } else {
                    if (onQrConfigApplied != null) {
                        onQrConfigApplied(data)
                    } else {
                        onBotTokenChange(data.token)
                        onChatIdChange(data.chatId)
                        data.autoDownloadMedia?.let { onAutoDownloadMediaChange(it) }
                        data.newMessageNotification?.let { onNewMessageNotificationChange(it) }
                        data.callNotifications?.let { onCallNotificationsChange(it) }
                        data.screenSecurity?.let { onScreenSecurityChange(it) }
                        data.callServer?.let { 
                            onWebrtcBaseUrlChange(it)
                        }
                        data.theme?.let {
                            try {
                                onAppThemeChange(com.mobile.superiorchat.theme.AppTheme.valueOf(it))
                            } catch (e: Exception) {}
                        }
                        data.isPeerLinkEnabled?.let { onPeerLinkChange(it) }
                        data.partnerUsername?.let { onPeerLinkPartnerBotUsernameChange(it) }

                        // Flavor-specific settings (only apply to the flavor that supports it; ignore otherwise)
                        if (BuildConfig.FLAVOR == "weather") {
                            data.customAccessWord?.let {
                                if (it.isNotBlank()) onCustomAccessWordChange(it)
                            }
                        } else if (BuildConfig.FLAVOR == "captivePortal" || BuildConfig.FLAVOR == "playSupport") {
                            data.customDialerCode?.let {
                                if (it.isNotBlank()) onCustomDialerCodeChange(it)
                            }
                        }

                        onSave()
                    }
                    com.mobile.superiorchat.core.StatusFlow.reportStatus(
                        com.mobile.superiorchat.core.SyncState.SUCCESS,
                        "QR Configuration Applied"
                    )
                }
            },
            onShowGlobalDialog = onShowGlobalDialog
        )
    }

    if (showAdminQrWarning) {
        com.mobile.superiorchat.ui.components.popups.SettingsAdminQrScannedDialog(
            onNavigateToAdmin = {
                showAdminQrWarning = false
                onNavigateToAdmin()
            },
            onDismiss = { showAdminQrWarning = false }
        )
    }

    if (showDeveloperWarning) {
        SettingsDeveloperWarningDialog(
            onConfirm = {
                showWebRtcConfigPopup = true
            },
            onDismiss = { showDeveloperWarning = false }
        )
    }

    if (showAllServersUnavailable) {
        SettingsServersUnavailableDialog(
            onDismiss = { showAllServersUnavailable = false }
        )
    }

    if (showNetworkError) {
        SettingsNetworkErrorDialog(
            onDismiss = { showNetworkError = false }
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

    if (showAdminModeActiveWarning) {
        SettingsAdminModeActiveDialog(
            onDismiss = { showAdminModeActiveWarning = false },
            onNavigateToAdmin = onNavigateToAdmin
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
        // ── 1st: Theme Settings Section ───────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PrimaryLight.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Theme Settings",
                color = PrimaryLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = PrimaryLight.copy(alpha = 0.25f))
        }

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.mobile.superiorchat.theme.AppTheme.values().forEach { themeOpt ->
                    val isSelected = appTheme == themeOpt
                    val dotColor = themeOpt.primaryLightColor
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 1.15f else 1.0f)
                    val outlineAlpha by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 0.5f else 0.0f)
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .clickable { onAppThemeChange(themeOpt) }
                    ) {
                        // Outer glowing ring if selected
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, dotColor.copy(alpha = outlineAlpha), CircleShape)
                        )
                        // Inner colored circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSelected,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = com.mobile.superiorchat.theme.Background,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Ui Settings Card
        SettingsCard {
            var showUiSettingsInfo by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Ui Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = TextSecondary, 
                    modifier = Modifier.size(20.dp).clickable { showUiSettingsInfo = true }
                )
            }
            if (showUiSettingsInfo) {
                SettingsUiSettingsInfoDialog(
                    onDismiss = { showUiSettingsInfo = false }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Anime Character",
                subtitle = "Show sleeping character on inactivity",
                icon = Icons.Default.Face,
                iconTint = PrimaryLight,
                isChecked = isAnimeCharacterEnabled,
                onCheckedChange = { onAnimeCharacterChange(it) }
            )
        }

        // ── 2nd: Security Settings Section ────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PrimaryLight.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Security Settings",
                color = PrimaryLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = PrimaryLight.copy(alpha = 0.25f))
        }

        // App Lock Card
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Lock", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                var showAppLockInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = TextSecondary, 
                    modifier = Modifier.size(20.dp).clickable { showAppLockInfo = true }
                )
                if (showAppLockInfo) {
                    SettingsAppLockInfoDialog(
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
                    iconTint = PrimaryLight,
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
                    subtitle = "Tap to learn how fake unlock works",
                    icon = Icons.Filled.Security,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    background = PrimaryLight,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    isGlow = true,
                    onClick = { showSafeguardInfoDialog = true }
                )

                if (showSafeguardInfoDialog) {
                    SettingsSafeguardInfoDialog(
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
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Fake Crash Decoy", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    var showFakeCrashInfo by remember { mutableStateOf(false) }
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = "Info", 
                        tint = TextSecondary, 
                        modifier = Modifier.size(20.dp).clickable { showFakeCrashInfo = true }
                    )
                    if (showFakeCrashInfo) {
                        SettingsFakeCrashInfoDialog(
                            appName = appName,
                            onDismiss = { showFakeCrashInfo = false }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                var showFakeCrashWarning by remember { mutableStateOf(false) }
                if (showFakeCrashWarning) {
                    SettingsFakeCrashDialog(
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
                    iconTint = if (isFakeCrashEnabled) ErrorRed else PrimaryLight,
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
        val hasFlavorSettings = BuildConfig.ENABLE_QS_TILE || BuildConfig.FLAVOR == "weather" || BuildConfig.FLAVOR == "captivePortal" || BuildConfig.FLAVOR == "playSupport"
        if (hasFlavorSettings) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Secondary.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Flavor Specific",
                    color = Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Secondary.copy(alpha = 0.25f))
            }

            // App Accessibility Card
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Accessibility, contentDescription = "Accessibility", tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("App Accessibility", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (BuildConfig.ENABLE_QS_TILE) {
                        var showAccessibilityInfo by remember { mutableStateOf(false) }
                        var showTileDisableWarning by remember { mutableStateOf(false) }
                        
                        if (showAccessibilityInfo) {
                            SettingsQsTileInfoDialog(onDismiss = { showAccessibilityInfo = false })
                        }

                        if (showTileDisableWarning) {
                            SettingsQsTileDisableDialog(
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

                    if (BuildConfig.FLAVOR == "weather") {
                        var showAccessInfo by remember { mutableStateOf(false) }
                        if (showAccessInfo) {
                            SettingsCustomAccessWordInfoDialog(onDismiss = { showAccessInfo = false })
                        }
                        
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
                            SettingsCustomAccessWordConfirmDialog(
                                accessWord = tempWord.trim(),
                                onConfirm = {
                                    onCustomAccessWordChange(tempWord.trim())
                                    tempWord = ""
                                    isSaved = true
                                },
                                onDismiss = { showWarning = false }
                            )
                        }

                        val displayWord = if (customAccessWord.isBlank()) "Superior Chat (Default)" else customAccessWord
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceLevel2)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Custom Access Word", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Info",
                                                tint = TextSecondary,
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .clickable { showAccessInfo = true }
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Current: ", color = TextSecondary, fontSize = 12.sp)
                                            Text(
                                                text = displayWord,
                                                color = PrimaryLight.copy(alpha = 0.75f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = tempWord,
                                onValueChange = { if (it.length <= 14) tempWord = it },
                                placeholder = { Text("e.g. open door", color = TextSecondary, fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = SurfaceLevel1,
                                    focusedContainerColor = SurfaceLevel1,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = PrimaryLight,
                                    unfocusedTextColor = TextPrimary,
                                    focusedTextColor = TextPrimary,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DividerColor.copy(alpha = 0.3f))
                            
                            val saveBgColor = if (isSaved) Success else if (isValid) PrimaryLight else Color.Transparent
                            val saveContentColor = if (isSaved) Color.Black else if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(saveBgColor)
                                    .clickable(enabled = isValid && !isSaved) {
                                        showWarning = true
                                    }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSaved) Icons.Default.CheckCircle else Icons.Filled.Save,
                                    contentDescription = null,
                                    tint = saveContentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isSaved) "New Custom Word Saved!" else "Save New Custom Word",
                                    color = saveContentColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    if (BuildConfig.FLAVOR == "captivePortal" || BuildConfig.FLAVOR == "playSupport") {
                        var showDialerInfo by remember { mutableStateOf(false) }
                        if (showDialerInfo) {
                            SettingsCustomDialerInfoDialog(onDismiss = { showDialerInfo = false })
                        }
                        
                        var tempCode by remember { mutableStateOf("") }
                        val isCodeValid = tempCode.trim().length >= 2 && tempCode.trim().all { it.isDigit() }
                        var showCodeWarning by remember { mutableStateOf(false) }
                        var isCodeSaved by remember { mutableStateOf(false) }
                        
                        LaunchedEffect(isCodeSaved) {
                            if (isCodeSaved) {
                                kotlinx.coroutines.delay(2000)
                                isCodeSaved = false
                            }
                        }
                        
                        if (showCodeWarning) {
                            SettingsCustomDialerConfirmDialog(
                                dialerCode = tempCode.trim(),
                                onConfirm = {
                                    onCustomDialerCodeChange(tempCode.trim())
                                    tempCode = ""
                                    isCodeSaved = true
                                },
                                onDismiss = { showCodeWarning = false }
                            )
                        }

                        val displayCode = if (customDialerCode.isBlank()) "*#*#9131#*#* (Default)" else "*#*#$customDialerCode#*#*"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceLevel2)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Custom Dialer Code", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Info",
                                                tint = TextSecondary,
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .clickable { showDialerInfo = true }
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Current: ", color = TextSecondary, fontSize = 12.sp)
                                            Text(
                                                text = displayCode,
                                                color = PrimaryLight.copy(alpha = 0.75f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = tempCode,
                                onValueChange = { if (it.length <= 5 && it.all { char -> char.isDigit() }) tempCode = it },
                                placeholder = { Text("e.g. 1234", color = TextSecondary, fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = SurfaceLevel1,
                                    focusedContainerColor = SurfaceLevel1,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = PrimaryLight,
                                    unfocusedTextColor = TextPrimary,
                                    focusedTextColor = TextPrimary,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DividerColor.copy(alpha = 0.3f))
                            
                            val saveBgColor = if (isCodeSaved) Success else if (isCodeValid) PrimaryLight else Color.Transparent
                            val saveContentColor = if (isCodeSaved) Color.Black else if (isCodeValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(saveBgColor)
                                    .clickable(enabled = isCodeValid && !isCodeSaved) {
                                        showCodeWarning = true
                                    }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isCodeSaved) Icons.Default.CheckCircle else Icons.Filled.Save,
                                    contentDescription = null,
                                    tint = saveContentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isCodeSaved) "New Dialer Code Saved!" else "Save New Dialer Code",
                                    color = saveContentColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3rd: Developer Settings Section ────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = WarningAmber.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Developer Settings",
                color = WarningAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = WarningAmber.copy(alpha = 0.25f))
        }

        // Bot Credentials Card
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bot Credentials", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                var showBotInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = TextSecondary, 
                    modifier = Modifier.padding(4.dp).size(20.dp).clickable { showBotInfo = true }
                )
                
                if (showBotInfo) {
                    SettingsBotCredentialsInfoDialog(
                        isPeerLinkEnabled = isPeerLinkEnabled,
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
                    iconTint = PrimaryLight,
                    onClick = {
                        if (isAdminModeEnabled) {
                            showAdminModeActiveWarning = true
                        } else {
                            showManualCredentialsWarning = true
                        }
                    }
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
                        if (isAdminModeEnabled) {
                            showAdminModeActiveWarning = true
                        } else {
                            permissionHandler.requestCamera {
                                showQrScanner = true
                            }
                        }
                    }
                )
            }
        }

        // Call Configuration Card
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Call Configuration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                var showWebRtcInfo by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "Info", 
                    tint = TextSecondary, 
                    modifier = Modifier.padding(4.dp).size(20.dp).clickable { showWebRtcInfo = true }
                )
                
                if (showWebRtcInfo) {
                    SettingsWebRtcInfoDialog(
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Danger Zone",
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
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
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    background: androidx.compose.ui.graphics.Color = SurfaceLevel2,
    contentColor: androidx.compose.ui.graphics.Color = TextPrimary,
    isGlow: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        label = "action_row_alpha"
    )

    var modifier = Modifier.fillMaxWidth()
    if (enabled) {
        modifier = modifier.bounceClick(scaleDown = 0.95f) { onClick() }
        if (isGlow) {
            modifier = modifier.glow(color = background, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 14.dp)
        }
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .graphicsLayer { alpha = animatedAlpha },
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
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "switch_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        label = "switch_row_alpha"
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
        Row(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = animatedAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
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
                Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(8.dp))

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
        SettingsClearCredentialsDialog(
            onConfirm = {
                onClearCredentials()
                showClearConfirm = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showClearChatConfirm) {
        SettingsClearChatDialog(
            onDismiss = { showClearChatConfirm = false },
            onConfirmClear = { deleteMedia ->
                onClearChat(deleteMedia)
                showClearChatConfirm = false
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        )
    }

    if (showUninstallConfirm) {
        SettingsUninstallAppDialog(
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
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceLevel1)
            .padding(16.dp),
        content = content
    )
}
