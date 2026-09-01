package com.mobile.superiorchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.popups.CredentialsPopup
import com.mobile.superiorchat.ui.components.popups.AdminPeerLinkInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminLifecycleInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminIAmAdminInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminAppToAppGuideDialog
import com.mobile.superiorchat.ui.components.popups.AdminCredentialsInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminDisableRouteMessagesDialog
import com.mobile.superiorchat.ui.components.popups.AdminDisableAdminModeDialog
import com.mobile.superiorchat.ui.components.popups.AdminPipRequiredDialog
import com.mobile.superiorchat.ui.components.popups.AdminNotificationRequiredDialog
import com.mobile.superiorchat.ui.components.popups.AdminReadOnlyInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminClientQrScannedDialog
import androidx.core.app.NotificationManagerCompat

@Composable
fun AdminSettingsScreen(
    viewModel: MainViewModel,
    onShowGlobalDialog: (GlobalDialogState) -> Unit
) {
    val context = LocalContext.current
    var showSetupDialog by remember { mutableStateOf(false) }
    var showPeerLinkInfo by remember { mutableStateOf(false) }
    var showLifecycleInfo by remember { mutableStateOf(false) }
    var showIAmAdminInfo by remember { mutableStateOf(false) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }
    var showCredentialsInfo by remember { mutableStateOf(false) }
    var showDisableRouteWarning by remember { mutableStateOf(false) }
    var showDisableAdminModeWarning by remember { mutableStateOf(false) }
    var showPipRequired by remember { mutableStateOf(false) }
    var showNotificationRequired by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showReadOnlyInfo by remember { mutableStateOf(false) }
    var showClientQrWarning by remember { mutableStateOf(false) }

    if (showReadOnlyInfo) {
        AdminReadOnlyInfoDialog(
            isHardLocked = viewModel.isPeerLinkHardLocked,
            onDismiss = { showReadOnlyInfo = false }
        )
    }

    if (showQrScanner) {
        com.mobile.superiorchat.ui.components.QrScanner(
            onDismiss = { showQrScanner = false },
            onSuccess = { data ->
                showQrScanner = false
                if (data.role == "CLIENT" || (data.isAdminModeEnabled != true && data.role != "ADMIN")) {
                    showClientQrWarning = true
                } else {
                    viewModel.applyQrConfig(data)
                    com.mobile.superiorchat.core.StatusFlow.reportStatus(
                        com.mobile.superiorchat.core.SyncState.SUCCESS,
                        "Admin QR Configuration Applied"
                    )
                }
            },
            onShowGlobalDialog = onShowGlobalDialog
        )
    }

    if (showClientQrWarning) {
        AdminClientQrScannedDialog(
            onDismiss = { showClientQrWarning = false }
        )
    }

    if (showPeerLinkInfo) {
        AdminPeerLinkInfoDialog(onDismiss = { showPeerLinkInfo = false })
    }

    if (showLifecycleInfo) {
        AdminLifecycleInfoDialog(onDismiss = { showLifecycleInfo = false })
    }


    if (showIAmAdminInfo) {
        AdminIAmAdminInfoDialog(onDismiss = { showIAmAdminInfo = false })
    }

    if (showSetupGuideDialog) {
        AdminAppToAppGuideDialog(onDismiss = { showSetupGuideDialog = false })
    }

    if (showCredentialsInfo) {
        AdminCredentialsInfoDialog(onDismiss = { showCredentialsInfo = false })
    }

    if (showDisableRouteWarning) {
        AdminDisableRouteMessagesDialog(
            onConfirm = {
                viewModel.togglePeerLink(false)
                showDisableRouteWarning = false
            },
            onDismiss = { showDisableRouteWarning = false }
        )
    }

    if (showDisableAdminModeWarning) {
        AdminDisableAdminModeDialog(
            onConfirm = {
                viewModel.toggleAdminMode(false)
                showDisableAdminModeWarning = false
            },
            onDismiss = { showDisableAdminModeWarning = false }
        )
    }

    if (showPipRequired) {
        AdminPipRequiredDialog(
            onGoToSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
            onDismiss = { showPipRequired = false }
        )
    }

    if (showNotificationRequired) {
        AdminNotificationRequiredDialog(
            onGoToSettings = {
                val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                }
                context.startActivity(intent)
            },
            onDismiss = { showNotificationRequired = false }
        )
    }

    if (showSetupDialog) {
        CredentialsPopup(
            initialToken = viewModel.botToken,
            initialChatId = viewModel.peerLinkGroupChatId.ifEmpty { viewModel.chatId },
            initialPartnerUsername = viewModel.peerLinkPartnerBotUsername,
            isPeerLinkEnabled = true,
            isAdminMode = true,
            onDismiss = { showSetupDialog = false },
            onSave = { token, chatId, partner ->
                viewModel.botToken = token
                viewModel.chatId = chatId
                viewModel.updatePeerLinkGroupId(chatId)
                viewModel.updatePeerLinkPartnerUsername(partner)
                viewModel.saveCredentials()
                showSetupDialog = false
                com.mobile.superiorchat.core.StatusFlow.reportStatus(
                    com.mobile.superiorchat.core.SyncState.SUCCESS,
                    "Admin Credentials Saved"
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: PeerLink
        val isLocked = viewModel.isPeerLinkLocked

        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Hub,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PeerLink",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showPeerLinkInfo = true }
                )
            }

            SettingsSwitchRow(
                title = "Read Only",
                subtitle = if (viewModel.isPeerLinkHardLocked) "Hard locked by setup QR" else "Lock all admin configuration",
                icon = Icons.Filled.Lock,
                iconTint = if (viewModel.isPeerLinkHardLocked) ErrorRed else PrimaryLight,
                isChecked = viewModel.isPeerLinkLocked,
                enabled = !viewModel.isPeerLinkHardLocked,
                onCheckedChange = { viewModel.togglePeerLinkLocked(it) }
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isLocked,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                val isHard = viewModel.isPeerLinkHardLocked
                val bannerColor = if (isHard) ErrorRed else PrimaryLight
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = bannerColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { showReadOnlyInfo = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHard) "Admin options Hard locked (By QR)" else "Admin options are locked (Read-Only)",
                            color = bannerColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Route Messages",
                subtitle = "Enable app to app chat via group bots",
                icon = Icons.Filled.SwapHoriz,
                iconTint = PrimaryLight,
                isChecked = viewModel.isPeerLinkEnabled,
                enabled = !isLocked,
                onCheckedChange = { checked ->
                    if (!checked) {
                        if (viewModel.peerLinkPartnerBotUsername.isNotEmpty() || viewModel.isAdminModeEnabled) {
                            showDisableRouteWarning = true
                        } else {
                            viewModel.togglePeerLink(false)
                        }
                    } else {
                        viewModel.togglePeerLink(true)
                    }
                }
            )
        }

        // Section 2: I Am Admin
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "I Am Admin",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showIAmAdminInfo = true }
                )
            }

            SettingsSwitchRow(
                title = "I will chat here",
                subtitle = "Enable this and fill Admin bot's credentials",
                icon = Icons.Filled.AdminPanelSettings,
                iconTint = PrimaryLight,
                isChecked = viewModel.isAdminModeEnabled,
                enabled = !isLocked && viewModel.isPeerLinkEnabled,
                onCheckedChange = { checked ->
                    if (!checked) {
                        if (viewModel.botToken.isNotEmpty()) {
                            showDisableAdminModeWarning = true
                        } else {
                            viewModel.toggleAdminMode(false)
                        }
                    } else {
                        viewModel.toggleAdminMode(true)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsActionRow(
                title = "App-to-App Setup Guide",
                subtitle = "Tap to learn how to configure two bots",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                background = PrimaryLight,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                isGlow = true,
                onClick = { showSetupGuideDialog = true }
            )
        }

        // Section 3: Credentials (Always visible, disabled when I Am Admin is false)
        val isCredentialsEnabled = !isLocked && viewModel.isAdminModeEnabled

        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Credentials",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showCredentialsInfo = true }
                )
            }

            val isConfigured = viewModel.botToken.isNotEmpty()
                && (viewModel.peerLinkGroupChatId.isNotEmpty() || viewModel.chatId.isNotEmpty())
                && viewModel.peerLinkPartnerBotUsername.isNotEmpty()
            SettingsActionRow(
                title = if (isConfigured) "Edit Manually" else "Add Manually",
                subtitle = "Configure Bot, Group & Partner",
                icon = if (isConfigured) Icons.Filled.Edit else Icons.Filled.Add,
                iconTint = PrimaryLight,
                enabled = isCredentialsEnabled,
                onClick = { showSetupDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsActionRow(
                title = "Scan QR Code",
                subtitle = "Import configuration from Setup App",
                icon = Icons.Filled.QrCodeScanner,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                background = PrimaryLight,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                isGlow = true,
                enabled = isCredentialsEnabled,
                onClick = { showQrScanner = true }
            )
        }

        // Section 4: Lifecycle
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lifecycle",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showLifecycleInfo = true }
                )
            }

            SettingsSwitchRow(
                title = "Hide from Recent",
                subtitle = "Exclude app from Android recent apps overview",
                icon = Icons.Filled.VisibilityOff,
                iconTint = PrimaryLight,
                isChecked = viewModel.isExcludeFromRecentsEnabled,
                enabled = !isLocked,
                onCheckedChange = { viewModel.toggleExcludeFromRecents(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Background Calls",
                subtitle = "Keep calls active and enable Picture-in-Picture when app is minimized",
                icon = Icons.Filled.PhoneInTalk,
                iconTint = PrimaryLight,
                isChecked = viewModel.isBackgroundCallsEnabled,
                enabled = !isLocked,
                onCheckedChange = { checked ->
                    if (checked) {
                        val permStatus = viewModel.permissionStatus.value
                        if (!permStatus.hasPipPermission) {
                            showPipRequired = true
                        } else {
                            viewModel.toggleBackgroundCalls(true)
                        }
                    } else {
                        viewModel.toggleBackgroundCalls(false)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(30.dp)) // Bottom padding
    }
}

