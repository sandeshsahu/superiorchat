package com.mobile.superiorchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.popups.PeerLinkSetupFlowDialog

@Composable
fun AdminSettingsScreen(
    viewModel: MainViewModel,
    onShowGlobalDialog: (GlobalDialogState) -> Unit
) {
    var showSetupDialog by remember { mutableStateOf(false) }

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
                onCheckedChange = { viewModel.togglePeerLinkLocked(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Route Messages",
                subtitle = "Enable app to app chat via group bots",
                icon = Icons.Filled.SwapHoriz,
                iconTint = PrimaryLight,
                isChecked = viewModel.isPeerLinkEnabled,
                onCheckedChange = { if (!viewModel.isPeerLinkLocked) viewModel.togglePeerLink(it) }
            )
        }

        // Section 2: I Am Admin
        val isLocked = viewModel.isPeerLinkLocked

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
                onCheckedChange = {
                    if (!isLocked && viewModel.isPeerLinkEnabled) {
                        viewModel.toggleAdminMode(it)
                    }
                }
            )
        }

        // Section 3: Credentials (Only visible if isAdminModeEnabled is true)
        if (viewModel.isAdminModeEnabled) {
            SettingsCard {
                Text(
                    text = "Credentials",
                    fontSize = 14.sp,
                    color = PrimaryLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                val isConfigured = viewModel.botToken.isNotEmpty()
                SettingsActionRow(
                    title = if (isConfigured) "Edit Manually" else "Add Manually",
                    subtitle = "Configure Bot, Group & Partner",
                    icon = if (isConfigured) Icons.Filled.Edit else Icons.Filled.Add,
                    iconTint = PrimaryLight,
                    onClick = { if (!isLocked) showSetupDialog = true }
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
                    onClick = { /* Disabled/No-op for now as per plan */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp)) // Bottom padding
    }
}
