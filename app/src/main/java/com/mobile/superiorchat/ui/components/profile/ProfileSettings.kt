package com.mobile.superiorchat.ui.components.profile

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import com.mobile.superiorchat.ui.components.bounceClick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.DividerColor
import com.mobile.superiorchat.theme.ErrorRed
import com.mobile.superiorchat.theme.InfoBlue
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary
import com.mobile.superiorchat.theme.Background

import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Message

enum class ProfileSheetState { MAIN, CHAT_SETTINGS, NOTIFICATIONS, PRIVACY_SECURITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    hasCredentials: Boolean,
    onDismiss: () -> Unit,
    onNavigateToAppSettings: (() -> Unit)?,
    isAutoDownloadMediaEnabled: Boolean,
    isScreenSecurityEnabled: Boolean,
    isNewMessageNotificationEnabled: Boolean,
    isAppNotificationsEnabled: Boolean,
    onAutoDownloadMediaChange: (Boolean) -> Unit,
    onScreenSecurityChange: (Boolean) -> Unit,
    onNewMessageNotificationChange: (Boolean) -> Unit,
    onAppNotificationsChange: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    var currentSheetState by remember { mutableStateOf(ProfileSheetState.MAIN) }

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
        BackHandler(enabled = currentSheetState != ProfileSheetState.MAIN) {
            currentSheetState = ProfileSheetState.MAIN
        }

        AnimatedContent(
            targetState = currentSheetState,
            transitionSpec = {
                val isForward = when (targetState) {
                    ProfileSheetState.MAIN -> false
                    else -> true
                }
                (slideInHorizontally(animationSpec = tween(300)) { width -> if (isForward) width else -width } +
                    fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(animationSpec = tween(300)) { width -> if (isForward) -width else width } +
                    fadeOut(animationSpec = tween(300)))
            },
            label = "sheet_content_anim"
        ) { state ->
            when (state) {
                ProfileSheetState.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("Profile Settings", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Manage your profile preferences", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    SettingsSheetRow(
                        icon = Icons.Filled.Chat,
                        iconTint = PrimaryLight,
                        title = "Chat Settings",
                        subtitle = "Media, screenshots, and more",
                        onClick = { currentSheetState = ProfileSheetState.CHAT_SETTINGS }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSheetRow(
                        icon = Icons.Filled.Notifications,
                        iconTint = PrimaryLight,
                        title = "Notifications",
                        subtitle = "Alerts and stealth settings",
                        onClick = { currentSheetState = ProfileSheetState.NOTIFICATIONS }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Privacy & Security section
                    SettingsSheetRow(
                        icon = Icons.Filled.Shield,
                        iconTint = InfoBlue,
                        title = "Privacy & Security",
                        subtitle = "Screen security and privacy",
                        onClick = { currentSheetState = ProfileSheetState.PRIVACY_SECURITY }
                    )
                }
                }
                
                ProfileSheetState.CHAT_SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Back button and Chat Settings header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SurfaceLevel2)
                                    .bounceClick { currentSheetState = ProfileSheetState.MAIN },
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
                                Text("Chat Settings", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Media and security", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Toggles
                        var showAutoDownloadInfo by remember { mutableStateOf(false) }
                        SettingsSwitchRow(
                            icon = Icons.Filled.CloudDownload,
                            iconTint = PrimaryLight,
                            title = "Auto-Download Media",
                            subtitle = "Automatically download photos/videos",
                            isChecked = isAutoDownloadMediaEnabled,
                            onCheckedChange = onAutoDownloadMediaChange,
                            onInfoClick = { showAutoDownloadInfo = true }
                        )

                        if (showAutoDownloadInfo) {
                            com.mobile.superiorchat.ui.components.popups.InfoDialog(
                                title = "Auto-Download Media",
                                message = "When enabled, photos and videos will automatically download when you receive them in chat. \n\nTurn this off to save mobile data.",
                                onDismiss = { showAutoDownloadInfo = false }
                            )
                        }
                    }
                }

                ProfileSheetState.PRIVACY_SECURITY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Back button and Privacy Settings header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SurfaceLevel2)
                                    .bounceClick { currentSheetState = ProfileSheetState.MAIN },
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
                                Text("Privacy & Security", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Screen security and locking", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        var showSecurityInfo by remember { mutableStateOf(false) }
                        SettingsSwitchRow(
                            icon = Icons.Filled.Security,
                            iconTint = InfoBlue,
                            title = "Block Screenshots",
                            subtitle = "Prevent screen capture and recording",
                            isChecked = isScreenSecurityEnabled,
                            onCheckedChange = onScreenSecurityChange,
                            onInfoClick = { showSecurityInfo = true }
                        )

                        if (showSecurityInfo) {
                            com.mobile.superiorchat.ui.components.popups.InfoDialog(
                                title = "Screen Security",
                                message = "This prevents any app, screen recorder, or screen cast from capturing the chat. \n\n*Screenshots* will appear pure black.",
                                onDismiss = { showSecurityInfo = false }
                            )
                        }
                    }
                }

                ProfileSheetState.NOTIFICATIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SurfaceLevel2)
                                    .bounceClick { currentSheetState = ProfileSheetState.MAIN },
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
                                Text("Notifications", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Alerts and stealth rules", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        var showDisableNotificationsDialog by remember { mutableStateOf(false) }

                        var showAppNotificationsInfo by remember { mutableStateOf(false) }

                        SettingsSwitchRow(
                            icon = Icons.Filled.NotificationsActive,
                            iconTint = PrimaryLight,
                            title = "App Notifications",
                            subtitle = "Allow decoy app to show notifications",
                            isChecked = isAppNotificationsEnabled,
                            onCheckedChange = {
                                if (!it) {
                                    showDisableNotificationsDialog = true
                                } else {
                                    // If they are trying to enable it, we also must redirect to settings
                                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onInfoClick = { showAppNotificationsInfo = true }
                        )
                        
                        if (showAppNotificationsInfo) {
                            com.mobile.superiorchat.ui.components.popups.InfoDialog(
                                title = "App Notifications",
                                message = "Controls the underlying Android System notification permissions.\n\nWhen disabled, the app is completely blocked from showing *Any background notifications*, making it ultra-stealthy. *Background sync* will still work perfectly.",
                                onDismiss = { showAppNotificationsInfo = false }
                            )
                        }
                        
                        if (showDisableNotificationsDialog) {
                            com.mobile.superiorchat.ui.components.popups.ActionDialog(
                                title = "Disable App Notifications",
                                message = "To completely disable notifications without crashing the background service, you must turn them off from Android's System Settings.\n\nClick Proceed to open the *App Info* page, then tap *Notifications* and turn them off.",
                                confirmText = "Proceed",
                                dismissText = "Cancel",
                                icon = Icons.Filled.Notifications,
                                iconTint = PrimaryLight,
                                onConfirm = {
                                    showDisableNotificationsDialog = false
                                    onAppNotificationsChange(false) // Save explicit intent to bypass startup prompt
                                    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                },
                                onDismiss = {
                                    showDisableNotificationsDialog = false
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var showNotificationInfo by remember { mutableStateOf(false) }
                        SettingsSwitchRow(
                            icon = Icons.Filled.Message,
                            iconTint = PrimaryLight,
                            title = "New Messages",
                            subtitle = "Visual decoy changes for messages",
                            isChecked = isNewMessageNotificationEnabled,
                            onCheckedChange = onNewMessageNotificationChange,
                            onInfoClick = { showNotificationInfo = true }
                        )

                        if (showNotificationInfo) {
                            com.mobile.superiorchat.ui.components.popups.InfoDialog(
                                title = "New Message Notifications",
                                message = "When *Enabled*, the stealth app's background service notification will visually change states (e.g., \"*Live Update*\" or \"*Heavy data usage detected*\") to alert you of new incoming messages.\n\nWhen *Disabled*, messages will still sync silently in the background, but the decoy *notification* will never change its idle state.",
                                onDismiss = { showNotificationInfo = false }
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun SettingsSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.ChevronRight,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "row_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(trailingIcon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    title: String,
    subtitle: String,
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
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
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
