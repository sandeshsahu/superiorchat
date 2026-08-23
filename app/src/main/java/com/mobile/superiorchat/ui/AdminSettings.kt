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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.popups.PeerLinkSetupFlowDialog
import com.mobile.superiorchat.ui.components.popups.AdminReadOnlyInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminRouteMessagesInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminIAmAdminInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminAppToAppGuideDialog
import com.mobile.superiorchat.ui.components.popups.AdminCredentialsInfoDialog
import com.mobile.superiorchat.ui.components.popups.AdminExcludeFromRecentsInfoDialog

@Composable
fun AdminSettingsScreen(
    viewModel: MainViewModel,
    onShowGlobalDialog: (GlobalDialogState) -> Unit
) {
    var showSetupDialog by remember { mutableStateOf(false) }
    var showReadOnlyInfo by remember { mutableStateOf(false) }
    var showRouteMessagesInfo by remember { mutableStateOf(false) }
    var showIAmAdminInfo by remember { mutableStateOf(false) }
    var showSetupGuideDialog by remember { mutableStateOf(false) }
    var showCredentialsInfo by remember { mutableStateOf(false) }
    var showExcludeFromRecentsInfo by remember { mutableStateOf(false) }

    if (showReadOnlyInfo) {
        AdminReadOnlyInfoDialog(onDismiss = { showReadOnlyInfo = false })
    }

    if (showRouteMessagesInfo) {
        AdminRouteMessagesInfoDialog(onDismiss = { showRouteMessagesInfo = false })
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

    if (showExcludeFromRecentsInfo) {
        AdminExcludeFromRecentsInfoDialog(onDismiss = { showExcludeFromRecentsInfo = false })
    }

    if (showSetupDialog) {
        PeerLinkSetupFlowDialog(
            initialBotToken = viewModel.botToken,
            initialGroupChatId = viewModel.peerLinkGroupChatId,
            initialPartnerUsername = viewModel.peerLinkPartnerBotUsername,
            onComplete = { botToken, groupChatId, partnerUsername ->
                viewModel.botToken = botToken
                viewModel.updatePeerLinkGroupId(groupChatId)
                viewModel.updatePeerLinkPartnerUsername(partnerUsername)
                viewModel.saveCredentials()
                showSetupDialog = false
            },
            onDismiss = { showSetupDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Security Lock
        val isLocked = viewModel.isPeerLinkLocked

        SettingsCard {
            Text(
                text = "Security Lock",
                fontSize = 14.sp,
                color = PrimaryLight,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            SettingsSwitchRow(
                title = "Read Only",
                subtitle = "Lock all admin configuration",
                icon = Icons.Filled.Lock,
                iconTint = PrimaryLight,
                isChecked = viewModel.isPeerLinkLocked,
                onCheckedChange = { viewModel.togglePeerLinkLocked(it) },
                onInfoClick = { showReadOnlyInfo = true }
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isLocked,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = PrimaryLight.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = PrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin options are locked (Read-Only)",
                            color = PrimaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
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
                onCheckedChange = { viewModel.togglePeerLink(it) },
                onInfoClick = { showRouteMessagesInfo = true }
            )
        }

        // Section 2: Lifecycle
        SettingsCard {
            Text(
                text = "Lifecycle",
                fontSize = 14.sp,
                color = PrimaryLight,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            SettingsSwitchRow(
                title = "Hide from Recent",
                subtitle = "Exclude app from Android recent apps overview",
                icon = Icons.Filled.VisibilityOff,
                iconTint = PrimaryLight,
                isChecked = viewModel.isExcludeFromRecentsEnabled,
                enabled = !isLocked,
                onCheckedChange = { viewModel.toggleExcludeFromRecents(it) },
                onInfoClick = { showExcludeFromRecentsInfo = true }
            )
        }

        // Section 3: I Am Admin
        SettingsCard {
            Text(
                text = "I Am Admin",
                fontSize = 14.sp,
                color = PrimaryLight,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            SettingsSwitchRow(
                title = "I will chat here",
                subtitle = "Enable this and fill Admin bot's credentials",
                icon = Icons.Filled.AdminPanelSettings,
                iconTint = PrimaryLight,
                isChecked = viewModel.isAdminModeEnabled,
                enabled = !isLocked && viewModel.isPeerLinkEnabled,
                onCheckedChange = { viewModel.toggleAdminMode(it) },
                onInfoClick = { showIAmAdminInfo = true }
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

        // Section 3: Credentials (Only visible if isAdminModeEnabled is true)
        if (viewModel.isAdminModeEnabled) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Credentials",
                        fontSize = 14.sp,
                        color = PrimaryLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showCredentialsInfo = true }
                    )
                }

                val isConfigured = viewModel.botToken.isNotEmpty()
                SettingsActionRow(
                    title = if (isConfigured) "Edit Manually" else "Add Manually",
                    subtitle = "Configure Bot, Group & Partner",
                    icon = if (isConfigured) Icons.Filled.Edit else Icons.Filled.Add,
                    iconTint = PrimaryLight,
                    enabled = !isLocked,
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
                    enabled = !isLocked,
                    onClick = { /* Disabled/No-op for now as per plan */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp)) // Bottom padding
    }
}
