package com.mobile.superiorchat.ui.components.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import com.mobile.superiorchat.ui.components.ActionDialog
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
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    hasCredentials: Boolean,
    onDismiss: () -> Unit,
    onClearCredentials: () -> Unit,
    onNavigateToAppSettings: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current
    var showDangerOptions by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }

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
        AnimatedContent(
            targetState = showDangerOptions,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(300)) { width -> if (targetState) width else -width } +
                    fadeIn(animationSpec = tween(300))) togetherWith
                (slideOutHorizontally(animationSpec = tween(300)) { width -> if (targetState) -width else width } +
                    fadeOut(animationSpec = tween(300)))
            },
            label = "sheet_content_anim"
        ) { isDangerZone ->
            if (!isDangerZone) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("Profile Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Manage your profile preferences", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Future features placeholder row
                    SettingsSheetRow(
                        icon = Icons.Filled.Shield,
                        iconTint = InfoBlue,
                        title = "Privacy & Security",
                        subtitle = "Coming soon",
                        onClick = { /* future */ }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSheetRow(
                        icon = Icons.Filled.Notifications,
                        iconTint = PrimaryLight,
                        title = "Notifications",
                        subtitle = "Coming soon",
                        onClick = { /* future */ }
                    )

                    // Danger Zone — always visible, styled as red card
                    Spacer(modifier = Modifier.height(28.dp))

                    // Danger zone header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.2f))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Danger Zone", color = ErrorRed.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(10.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.2f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Danger Zone card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.07f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        SettingsSheetRow(
                            icon = Icons.Filled.Warning,
                            iconTint = ErrorRed,
                            title = "Danger Zone",
                            subtitle = "Sensitive actions located here",
                            titleColor = ErrorRed,
                            onClick = { showDangerOptions = true }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    // Back button and Danger Zone header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceLevel2)
                                .bounceClick { showDangerOptions = false },
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

                    // Clear Credentials Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.07f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        if (!hasCredentials) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.NoAccounts, null, tint = ErrorRed.copy(0.5f), modifier = Modifier.size(16.dp))
                                Text(
                                    "Credentials Empty",
                                    color = ErrorRed.copy(0.5f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            SettingsSheetRow(
                                icon = Icons.Filled.NoAccounts,
                                iconTint = ErrorRed,
                                title = "Clear Credentials",
                                subtitle = "Disconnect the bot and stop chatting",
                                titleColor = ErrorRed,
                                onClick = { showClearConfirm = true }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Uninstall App Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.07f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        SettingsSheetRow(
                            icon = Icons.Filled.DeleteForever,
                            iconTint = ErrorRed,
                            title = "Uninstall App",
                            subtitle = "Remove this application entirely",
                            titleColor = ErrorRed,
                            onClick = { showUninstallConfirm = true }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ActionDialog(
            title = "Clear Credentials",
            message = "This will disconnect the bot and stop the end-to-end chat. Both persons will lose access to the current chat session. Do you want to proceed?",
            icon = Icons.Filled.NoAccounts,
            iconTint = ErrorRed,
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                onClearCredentials()
                showClearConfirm = false
                onDismiss()
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showUninstallConfirm) {
        ActionDialog(
            title = "Uninstall App",
            message = "This will permanently remove the application from your device. Do you want to proceed?",
            icon = Icons.Filled.DeleteForever,
            iconTint = ErrorRed,
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}")))
                showUninstallConfirm = false
                onDismiss()
            },
            onDismiss = { showUninstallConfirm = false }
        )
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
