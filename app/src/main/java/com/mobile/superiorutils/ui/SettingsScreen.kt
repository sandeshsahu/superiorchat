package com.mobile.superiorutils.ui

import com.mobile.superiorutils.ui.components.GlassCard
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
import com.mobile.superiorutils.ui.components.bounceClick
import com.mobile.superiorutils.ui.components.glow
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
import com.mobile.superiorutils.theme.*


// ══════════════════════════════════════════════════════════
//  Settings Screen
// ══════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    isInternetConnected: Boolean,
    botToken: String,
    chatId: String,
    isAutoDownloadMediaEnabled: Boolean,
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onAutoDownloadMediaChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    var showCredentialsDialog by remember { mutableStateOf(false) }
    var tempBotToken by remember { mutableStateOf(botToken) }
    var tempChatId by remember { mutableStateOf(chatId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    if (errorMessage != null) {
        com.mobile.superiorutils.ui.components.ErrorDialog(
            title = "Invalid Credentials",
            message = errorMessage!!,
            onDismiss = { errorMessage = null }
        )
    }

    if (showCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showCredentialsDialog = false },
            title = { Text("Bot Credentials", color = TextPrimary) },
            text = {
                Column {
                    CredentialField("Bot Token", tempBotToken, { tempBotToken = it }, true, placeholder = "Enter Telegram Bot Token")
                    Spacer(modifier = Modifier.height(10.dp))
                    CredentialField("Target Chat ID", tempChatId, { tempChatId = it }, true, SecretIconType.EYE, "Enter Target Chat ID")
                }
            },
            containerColor = SurfaceLevel1,
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                Button(
                    onClick = {
                        val token = tempBotToken.trim()
                        val chat = tempChatId.trim()
                        
                        if (!com.mobile.superiorutils.utils.ValidationUtils.isValidBotToken(token)) {
                            errorMessage = "The Bot Token format is invalid. It should look like '1234567890:ABCdef...'"
                            return@Button
                        }
                        
                        if (!com.mobile.superiorutils.utils.ValidationUtils.isValidChatId(chat)) {
                            errorMessage = "The Chat ID format is invalid. It must be a numeric ID, optionally starting with a '-' sign."
                            return@Button
                        }
                        
                        onBotTokenChange(token)
                        onChatIdChange(chat)
                        showCredentialsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Save", color = TextPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) { Text("Cancel", color = TextSecondary) }
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
                val replaceText = if (isConfigured) "Replace Credentials" else "Add Credentials"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(SurfaceLevel2, RoundedCornerShape(12.dp))
                        .bounceClick(scaleDown = 0.95f) { showCredentialsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(replaceText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .glow(color = Primary, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 12.dp)
                        .background(PrimaryLight, RoundedCornerShape(12.dp))
                        .bounceClick(scaleDown = 0.95f) { onSave() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Update Credentials", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Normal)
                }
            }
        }

        // Media Preferences Card
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = "Media", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Media Preferences", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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

private enum class SecretIconType { LOCK, EYE }

@Composable
private fun CredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean = false,
    iconType: SecretIconType = SecretIconType.LOCK,
    placeholder: String = ""
) {
    var showSecret by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            singleLine = true,
            visualTransformation = if (isSecret && !showSecret) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isSecret) {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        val icon = if (iconType == SecretIconType.LOCK) {
                            if (showSecret) Icons.Default.LockOpen else Icons.Default.Lock
                        } else {
                            if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        }
                        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = DividerColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceLevel2,
                unfocusedContainerColor = SurfaceLevel2,
                cursorColor = Primary
            ),
            shape = RoundedCornerShape(10.dp)
        )
    }
}
