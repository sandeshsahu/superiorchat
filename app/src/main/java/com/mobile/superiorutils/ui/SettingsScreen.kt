package com.mobile.superiorutils.ui

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
import com.mobile.superiorutils.theme.*


// ══════════════════════════════════════════════════════════
//  Reusable Glass Card
// ══════════════════════════════════════════════════════════

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLevel1, RoundedCornerShape(24.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content
    )
}

// ══════════════════════════════════════════════════════════
//  Settings Screen
// ══════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    isInternetConnected: Boolean,
    botToken: String,
    chatId: String,
    onBotTokenChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    var showCredentialsDialog by remember { mutableStateOf(false) }
    var tempBotToken by remember { mutableStateOf(botToken) }
    var tempChatId by remember { mutableStateOf(chatId) }
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
                        onBotTokenChange(tempBotToken.trim())
                        onChatIdChange(tempChatId.trim())
                        showCredentialsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // System Checks Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = "Build", tint = Color(0xFFC7C4D7), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("System Checks", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E2E2))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Token Access", fontSize = 14.sp, color = Color(0xFFE2E2E2))
                }
                Text("Online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (isInternetConnected) Color(0xFF22C55E) else Color(0xFFEF4444), CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Internet Connectivity", fontSize = 14.sp, color = Color(0xFFE2E2E2))
                }
                Text(if (isInternetConnected) "Online" else "Offline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isInternetConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
            }
        }

        // Bot Credentials Card
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFFC7C4D7), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bot Credentials", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E2E2))
                }
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFC7C4D7), modifier = Modifier.size(20.dp))
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
                        .clickable { showCredentialsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(replaceText, color = Color(0xFFC7C4D7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(PrimaryLight, RoundedCornerShape(12.dp))
                        .clickable { onSave() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Update Credentials", color = Color(0xFF1000A9), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                }
            }
        }


        // About Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFC7C4D7), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("About", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E2E2))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B1B).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                InfoRow("App Name", "Superior Chat")
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                InfoRow("Author", "Sandesh")
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
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
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC7C4D7))
        Text(value, fontSize = 14.sp, color = Color(0xFFE2E2E2), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(0.6f))
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
