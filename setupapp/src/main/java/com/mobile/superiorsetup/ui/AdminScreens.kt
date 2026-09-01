package com.mobile.superiorsetup.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorsetup.core.AppManager
import com.mobile.superiorsetup.core.Config
import com.mobile.superiorsetup.core.Security
import com.mobile.superiorsetup.core.Validator
import com.mobile.superiorsetup.theme.*
import com.mobile.superiorsetup.ui.components.bounceClick
import com.mobile.superiorsetup.ui.components.glow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Admin Step 2: Generate Setup QR Hub Screen.
 * Decoupled action card with clear explanation of the upcoming chat mode choice.
 */
@Composable
fun AdminStep2Screen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Admin Setup", fontSize = 24.sp, color = TextPrimary)
            Text("Generate Setup QR", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Create a secure, encrypted setup QR code so your partner can instantly connect without technical hassle.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Action Card: Decoupled to show ONLY Setup QR Code heading and detail
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .glow(
                        color = PrimaryLight.copy(alpha = 0.15f),
                        radius = 16f,
                        dx = 0f,
                        dy = 6f,
                        cornerRadius = 20.dp
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.QrCode,
                                contentDescription = null,
                                tint = PrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Setup QR Code",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = PrimaryLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ADMIN TOOL",
                                color = PrimaryLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This setup tool configures encrypted messaging credentials and generates setup QR codes for instant on-device connection.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // What You Will Configure Next - Clear heading hierarchy with indented sub-items
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "What You Will Configure Next:",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                ) {
                    // Choice 1: Telegram Mode
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = PrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Chat on Telegram (1 Bot)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generates a Client QR code to scan on your partner's device. You message directly from your standard Telegram app in partner bot's private DM.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Choice 2: App-to-App Mode
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Devices,
                                contentDescription = null,
                                tint = PrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Both Use Superior Chat App (2 Bots)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generates an Admin QR to scan on your device, and a Client QR to scan on your partner's device. Unlocks extra features for you.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Setup Button & Security Note
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) { onNext() }
                    .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Start QR Setup",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("All credentials are encrypted with AES-GCM on-device.", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Admin Step 3: Chat Mode Decision Screen.
 * Allows the admin to select between:
 * 1. Standard Telegram Mode (1 bot, admin chats in Telegram DM)
 * 2. App-to-App Mode (2 bots, both chat inside Superior Chat app with rich extra features)
 */
@Composable
fun AdminStep3Screen(onNext: () -> Unit) {
    var selectedMode by remember { mutableStateOf(Config.adminChatMode.ifEmpty { "TELEGRAM" }) }
    var showTelegramInfo by remember { mutableStateOf(false) }
    var showAppToAppInfo by remember { mutableStateOf(false) }

    if (showTelegramInfo) {
        com.mobile.superiorsetup.ui.components.AdminTelegramModeInfoDialog(onDismiss = { showTelegramInfo = false })
    }

    if (showAppToAppInfo) {
        com.mobile.superiorsetup.ui.components.AdminAppToAppModeInfoDialog(onDismiss = { showAppToAppInfo = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Forum,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Choose Chat Mode", fontSize = 24.sp, color = TextPrimary)
            Text("How will you connect?", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select how you would like to connect and message with your partner.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: Chat from Telegram
            val isTelegramSelected = selectedMode == "TELEGRAM"
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isTelegramSelected) PrimaryLight else DividerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedMode = "TELEGRAM" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar with Checkbox on Left, Title in Center, Info Icon on Right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(
                                if (isTelegramSelected) {
                                    Modifier
                                        .glow(
                                            color = PrimaryLight.copy(alpha = 0.35f),
                                            radius = 16f,
                                            dx = 0f,
                                            dy = 6f,
                                            cornerRadius = 14.dp
                                        )
                                        .background(PrimaryLight, RoundedCornerShape(14.dp))
                                } else {
                                    Modifier
                                        .background(SurfaceLevel2, RoundedCornerShape(14.dp))
                                        .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Selection Checkbox on Left
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isTelegramSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (isTelegramSelected) MaterialTheme.colorScheme.onPrimaryContainer else DividerColor,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTelegramSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = if (isTelegramSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "I will chat on Telegram",
                                    color = if (isTelegramSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Info Icon on Right
                            IconButton(
                                onClick = { showTelegramInfo = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = if (isTelegramSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content below button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = PrimaryLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = PrimaryLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Fastest & Easiest",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You message directly from your standard Telegram app. Your partner uses the private Superior Chat app.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeFeaturePillDark("1 Bot")
                        ModeFeaturePillDark("No Group Needed")
                        ModeFeaturePillDark("1-Min Setup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Both on Superior Chat App
            val isAppToAppSelected = selectedMode == "APP_TO_APP"
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isAppToAppSelected) PrimaryLight else DividerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedMode = "APP_TO_APP" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar with Checkbox on Left, Title in Center, Info Icon on Right
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(
                                if (isAppToAppSelected) {
                                    Modifier
                                        .glow(
                                            color = PrimaryLight.copy(alpha = 0.35f),
                                            radius = 16f,
                                            dx = 0f,
                                            dy = 6f,
                                            cornerRadius = 14.dp
                                        )
                                        .background(PrimaryLight, RoundedCornerShape(14.dp))
                                } else {
                                    Modifier
                                        .background(SurfaceLevel2, RoundedCornerShape(14.dp))
                                        .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Selection Checkbox on Left
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isAppToAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (isAppToAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else DividerColor,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAppToAppSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    Icons.Filled.Devices,
                                    contentDescription = null,
                                    tint = if (isAppToAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else PrimaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Both use Superior Chat App",
                                    color = if (isAppToAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Info Icon on Right
                            IconButton(
                                onClick = { showAppToAppInfo = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = if (isAppToAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content below button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SurfaceLevel2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DividerColor)
                        ) {
                            Text(
                                text = "ADVANCED",
                                color = PrimaryLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Full Stealth for Both",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Both you and your partner chat inside the hidden Superior Chat app using secure group bot routing.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeFeaturePillDark("2 Bots")
                        ModeFeaturePillDark("Private Group")
                        ModeFeaturePillDark("Both in App")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue Button & Security Note
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        Config.adminChatMode = selectedMode
                        Config.adminIsPeerLinkEnabled = (selectedMode == "APP_TO_APP")
                        onNext()
                    }
                    .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Both options provide 100% private end-to-end messaging.", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModeFeaturePill(text: String, contentColor: Color) {
    Surface(
        color = contentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModeFeaturePillDark(text: String) {
    Surface(
        color = SurfaceLevel2,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Admin Step 4 Router: Dispatches to either Telegram Single-Bot screen or App-to-App 2-Bot wizard.
 */
@Composable
fun AdminStep4Screen(onNext: () -> Unit) {
    if (Config.adminChatMode == "APP_TO_APP") {
        AdminStep4AppToAppScreen(onNext = onNext)
    } else {
        AdminStep4TelegramScreen(onNext = onNext)
    }
}

/**
 * Step 4 (Telegram Mode): Single Bot Token & Personal Chat ID with Live Telegram Validation.
 */
@Composable
fun AdminStep4TelegramScreen(onNext: () -> Unit) {
    var botToken by remember { mutableStateOf(Config.adminTelegramBotToken) }
    var chatId by remember { mutableStateOf(Config.adminTelegramChatId) }
    var tokenVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var tokenApiError by remember { mutableStateOf<String?>(null) }
    var chatApiError by remember { mutableStateOf<String?>(null) }

    var showNetworkErrorDialog by remember { mutableStateOf(false) }
    var networkErrorMessage by remember { mutableStateOf("") }

    var isVerified by remember { mutableStateOf(false) }
    var extractedBotUsername by remember { mutableStateOf("") }
    var verifiedChatSummary by remember { mutableStateOf("") }

    val isTokenValid by remember(botToken) { derivedStateOf { botToken.isBlank() || Validator.isValidBotToken(botToken.trim()) } }
    val isChatIdValid by remember(chatId) { derivedStateOf { chatId.isBlank() || Validator.isValidChatId(chatId.trim()) } }
    val isGroupIdDetected by remember(chatId) { derivedStateOf { chatId.trim().startsWith("-") } }
    val isSyntaxInvalid by remember(isTokenValid, isChatIdValid) {
        derivedStateOf { !isTokenValid || !isChatIdValid }
    }
    val canVerify by remember(botToken, chatId, isSyntaxInvalid, isLoading, isVerified) {
        derivedStateOf { botToken.isNotBlank() && chatId.isNotBlank() && !isSyntaxInvalid && !isLoading && !isVerified }
    }

    val scope = rememberCoroutineScope()

    if (showNetworkErrorDialog) {
        com.mobile.superiorsetup.ui.components.AdminConnectionErrorDialog(
            message = if (networkErrorMessage.isNotEmpty()) networkErrorMessage else "Could not connect to Telegram servers. Please check your internet connection and try again.",
            onDismiss = { showNetworkErrorDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.VpnKey, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Telegram Credentials", fontSize = 24.sp, color = TextPrimary)
            Text("Bot Token & Chat ID", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter your Bot Token and Telegram Chat ID to connect your messaging session.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bot Token Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isTokenValid || tokenApiError != null) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = {
                            botToken = it
                            tokenApiError = null
                            validationError = null
                            isVerified = false
                            extractedBotUsername = ""
                            verifiedChatSummary = ""
                        },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        singleLine = true,
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isTokenValid || tokenApiError != null,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    if (!isTokenValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invalid token format. Must look like 1234567890:AAH...",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat ID Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isChatIdValid || chatApiError != null) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Your Telegram Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = {
                            chatId = it
                            chatApiError = null
                            validationError = null
                            isVerified = false
                            extractedBotUsername = ""
                            verifiedChatSummary = ""
                        },
                        placeholder = { Text("e.g. 1234567890", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        singleLine = true,
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isChatIdValid || chatApiError != null,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (!isChatIdValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chat ID must be a numeric ID (e.g. 1234567890)",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (isGroupIdDetected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = com.mobile.superiorsetup.theme.WarningAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, com.mobile.superiorsetup.theme.WarningAmber.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = com.mobile.superiorsetup.theme.WarningAmber,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Group ID Detected",
                                        color = com.mobile.superiorsetup.theme.WarningAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Direct 1-on-1 mode is designed for your personal Telegram User ID (Bot DM). Using groups here causes bugs. For group chat, select 'Both use Superior Chat App' in Step 3.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Bottom Slot: Replaces Instructions with Error or Verified Card
            if (validationError != null) {
                Surface(
                    color = ErrorRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(validationError!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            } else if (isVerified) {
                Surface(
                    color = Success.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Credentials Verified • Ready", color = Success, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (extractedBotUsername.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Bot: $extractedBotUsername", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        if (verifiedChatSummary.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("• $verifiedChatSummary", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Instructions Card
                Surface(
                    color = SurfaceLevel1,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("How to get these?", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Bot Token: Create a bot with @BotFather on Telegram\n• Chat ID: Message @userinfobot to get your numeric ID\n• Remember: Send /start to your bot from your Telegram app",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Area: 2-Stage Button Flow
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isVerified) {
                // Stage 2: Continue Button
                Button(
                    onClick = {
                        Config.adminTelegramBotToken = botToken.trim()
                        Config.adminTelegramChatId = chatId.trim()
                        Config.adminTelegramBotUsername = extractedBotUsername
                        Config.adminTelegramChatName = verifiedChatSummary
                        Config.adminChatMode = "TELEGRAM"
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLight,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Stage 1: Verify Button
                Button(
                    onClick = {
                        if (canVerify && !isLoading) {
                            isLoading = true
                            validationError = null
                            tokenApiError = null
                            chatApiError = null
                            scope.launch(Dispatchers.IO) {
                                val trimmedToken = botToken.trim()
                                val trimmedChat = chatId.trim()

                                val tokenRes = Validator.verifyBotToken(trimmedToken)
                                when (tokenRes) {
                                    is Validator.ValidationResult.NetworkError -> {
                                        withContext(Dispatchers.Main) {
                                            networkErrorMessage = tokenRes.message
                                            showNetworkErrorDialog = true
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Error -> {
                                        withContext(Dispatchers.Main) {
                                            tokenApiError = tokenRes.message
                                            validationError = tokenRes.message
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Success -> { /* Proceed */ }
                                }
                                val botUser = tokenRes.data
                                val botHandle = botUser.username?.let { "@$it" } ?: ""

                                val chatRes = Validator.verifyChatId(
                                    token = trimmedToken,
                                    chatId = trimmedChat,
                                    botUser = botUser,
                                    isPeerLinkEnabled = false,
                                    requireGroupOnly = false
                                )
                                when (chatRes) {
                                    is Validator.ValidationResult.NetworkError -> {
                                        withContext(Dispatchers.Main) {
                                            networkErrorMessage = chatRes.message
                                            showNetworkErrorDialog = true
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Error -> {
                                        withContext(Dispatchers.Main) {
                                            chatApiError = chatRes.message
                                            validationError = chatRes.message
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Success -> { /* Proceed */ }
                                }
                                val summary = chatRes.data

                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    extractedBotUsername = botHandle
                                    verifiedChatSummary = summary
                                    Config.adminTelegramChatName = summary
                                    Config.adminTelegramBotUsername = botHandle
                                    isVerified = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLight,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = SurfaceLevel2,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canVerify
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryLight, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Credentials...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (validationError != null) "Retry Verification" else "Verify Credentials",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Private • Encrypted • Hidden", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Step 4 (App-to-App Mode): Guided 2-Bot & Supergroup Configuration with Live Multi-Bot Validation.
 * Requires only: Partner's Bot Token, Your Bot Token, and Group Chat ID.
 * Automatically extracts usernames, validates group membership, administrator status, and disabled group privacy.
 */
@Composable
fun AdminStep4AppToAppScreen(onNext: () -> Unit) {
    var partnerToken by remember { mutableStateOf(Config.adminAppToAppPartnerBotToken) }
    var adminToken by remember { mutableStateOf(Config.adminAppToAppMyBotToken) }
    var groupChatId by remember { mutableStateOf(Config.adminAppToAppGroupId) }

    var partnerTokenVisible by remember { mutableStateOf(false) }
    var adminTokenVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }
    var partnerTokenApiError by remember { mutableStateOf<String?>(null) }
    var adminTokenApiError by remember { mutableStateOf<String?>(null) }
    var groupApiError by remember { mutableStateOf<String?>(null) }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showNetworkErrorDialog by remember { mutableStateOf(false) }
    var networkErrorMessage by remember { mutableStateOf("") }

    var partnerBotHandle by remember { mutableStateOf<String?>(Config.adminAppToAppPartnerBotUsername.takeIf { it.isNotEmpty() }) }
    var adminBotHandle by remember { mutableStateOf<String?>(Config.adminAppToAppMyBotUsername.takeIf { it.isNotEmpty() }) }
    var extractedPartnerUsername by remember { mutableStateOf(Config.adminAppToAppPartnerBotUsername) }
    var extractedAdminUsername by remember { mutableStateOf(Config.adminAppToAppMyBotUsername) }
    var verifiedGroupTitle by remember { mutableStateOf(Config.adminAppToAppGroupTitle) }

    val areTokensIdentical by remember(partnerToken, adminToken) {
        derivedStateOf {
            partnerToken.isNotBlank() && adminToken.isNotBlank() && partnerToken.trim().equals(adminToken.trim(), ignoreCase = false)
        }
    }

    val isPartnerTokenValid by remember(partnerToken, areTokensIdentical) {
        derivedStateOf { !areTokensIdentical && (partnerToken.isBlank() || Validator.isValidBotToken(partnerToken)) }
    }
    val isAdminTokenValid by remember(adminToken, areTokensIdentical) {
        derivedStateOf { !areTokensIdentical && (adminToken.isBlank() || Validator.isValidBotToken(adminToken)) }
    }
    val isGroupChatIdValid by remember(groupChatId) {
        derivedStateOf { groupChatId.isBlank() || Validator.isValidGroupChatId(groupChatId) }
    }

    val isSyntaxInvalid by remember(
        areTokensIdentical, isPartnerTokenValid, isAdminTokenValid, isGroupChatIdValid
    ) {
        derivedStateOf {
            areTokensIdentical || !isPartnerTokenValid || !isAdminTokenValid || !isGroupChatIdValid
        }
    }

    val canVerify by remember(partnerToken, adminToken, groupChatId, isSyntaxInvalid, isLoading, isVerified) {
        derivedStateOf {
            partnerToken.isNotBlank() && adminToken.isNotBlank() && groupChatId.isNotBlank() && !isSyntaxInvalid && !isLoading && !isVerified
        }
    }

    val scope = rememberCoroutineScope()

    if (showInfoDialog) {
        com.mobile.superiorsetup.ui.components.AdminBotSetupGuideDialog(onDismiss = { showInfoDialog = false })
    }

    if (showNetworkErrorDialog) {
        com.mobile.superiorsetup.ui.components.AdminConnectionErrorDialog(
            message = if (networkErrorMessage.isNotEmpty()) networkErrorMessage else "Could not connect to Telegram servers. Please check your internet connection and try again.",
            onDismiss = { showNetworkErrorDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Hub, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("App-to-App Setup", fontSize = 24.sp, color = TextPrimary)
            Text("2-Bot Group Credentials", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Provide both bot tokens and your private group ID. Handles, permissions, and group status are verified live.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Partner's Bot Token (User A side)
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (!isPartnerTokenValid || partnerTokenApiError != null || areTokensIdentical) ErrorRed
                    else if (partnerBotHandle != null && partnerToken.isNotBlank()) Success.copy(alpha = 0.6f)
                    else DividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partner's Bot Token (Her App)", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = partnerToken,
                        onValueChange = {
                            partnerToken = it
                            partnerTokenApiError = null
                            validationError = null
                            isVerified = false
                            partnerBotHandle = null
                            extractedPartnerUsername = ""
                            verifiedGroupTitle = ""
                        },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        singleLine = true,
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isPartnerTokenValid || partnerTokenApiError != null || areTokensIdentical,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (partnerTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { partnerTokenVisible = !partnerTokenVisible }) {
                                Icon(
                                    if (partnerTokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    if (areTokensIdentical) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Partner Bot and Admin Bot tokens cannot be identical.",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (partnerTokenApiError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = partnerTokenApiError!!,
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (!isPartnerTokenValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invalid token format. Must look like 1234567890:AAH...",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (partnerBotHandle != null && partnerToken.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Verified: $partnerBotHandle",
                            color = Success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Admin's Bot Token (User B side)
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (!isAdminTokenValid || adminTokenApiError != null || areTokensIdentical) ErrorRed
                    else if (adminBotHandle != null && adminToken.isNotBlank()) Success.copy(alpha = 0.6f)
                    else DividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Your Bot Token (Admin App)", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = adminToken,
                        onValueChange = {
                            adminToken = it
                            adminTokenApiError = null
                            validationError = null
                            isVerified = false
                            adminBotHandle = null
                            extractedAdminUsername = ""
                            verifiedGroupTitle = ""
                        },
                        placeholder = { Text("e.g. 9876543210:BBH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        singleLine = true,
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isAdminTokenValid || adminTokenApiError != null || areTokensIdentical,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (adminTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { adminTokenVisible = !adminTokenVisible }) {
                                Icon(
                                    if (adminTokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    if (areTokensIdentical) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Partner Bot and Admin Bot tokens cannot be identical.",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (adminTokenApiError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = adminTokenApiError!!,
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (!isAdminTokenValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Invalid token format. Must look like 1234567890:AAH...",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (adminBotHandle != null && adminToken.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Verified: $adminBotHandle",
                            color = Success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Private Group Chat ID Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (!isGroupChatIdValid || groupApiError != null) ErrorRed
                    else if (verifiedGroupTitle.isNotEmpty() && groupChatId.isNotBlank()) Success.copy(alpha = 0.6f)
                    else DividerColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Private Group Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = groupChatId,
                        onValueChange = {
                            groupChatId = it
                            groupApiError = null
                            validationError = null
                            isVerified = false
                            verifiedGroupTitle = ""
                        },
                        placeholder = { Text("e.g. -100123456789", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        singleLine = true,
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isGroupChatIdValid || groupApiError != null,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (groupApiError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = groupApiError!!,
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (!isGroupChatIdValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Group ID must start with '-' (e.g. -100123456789 or -123456789)",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (verifiedGroupTitle.isNotEmpty() && groupChatId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Verified Group: $verifiedGroupTitle",
                            color = Success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Bottom Slot: Replaces Checklist with Error or Verified Card
            if (validationError != null) {
                Surface(
                    color = ErrorRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(validationError!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            } else if (isVerified) {
                Surface(
                    color = Success.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All Checks Passed • Ready", color = Success, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Partner's Bot: $extractedPartnerUsername", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• Your Admin Bot: $extractedAdminUsername", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (verifiedGroupTitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("• Group: $verifiedGroupTitle", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Setup Checklist Guide Card with Info Button
                Surface(
                    color = SurfaceLevel1,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bot-to-Bot Setup Checklist", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = PrimaryLight.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { showInfoDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Guide", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Create 2 bots in @BotFather (one for you, one for partner)\n2. In @BotFather: Bot Settings -> Group Privacy -> Turn OFF for both bots\n3. In @BotFather: Bot Settings -> Allow Groups? -> Turn ON\n4. Create a Private Telegram Group and add both bots\n5. Promote BOTH bots to Administrator in Group Settings",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Area: 2-Stage Button Flow (Verify -> Continue)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isVerified) {
                // Stage 2: Continue Button
                Button(
                    onClick = {
                        Config.adminAppToAppPartnerBotToken = partnerToken.trim()
                        Config.adminAppToAppPartnerBotUsername = extractedPartnerUsername
                        Config.adminAppToAppMyBotToken = adminToken.trim()
                        Config.adminAppToAppMyBotUsername = extractedAdminUsername
                        Config.adminAppToAppGroupId = groupChatId.trim()
                        Config.adminAppToAppGroupTitle = verifiedGroupTitle
                        Config.adminIsPeerLinkEnabled = true
                        Config.adminChatMode = "APP_TO_APP"
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLight,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Stage 1: Verify Credentials Button
                Button(
                    onClick = {
                        if (canVerify && !isLoading) {
                            isLoading = true
                            validationError = null
                            adminTokenApiError = null
                            partnerTokenApiError = null
                            groupApiError = null

                            scope.launch(Dispatchers.IO) {
                                val trimmedPartner = partnerToken.trim()
                                val trimmedAdmin = adminToken.trim()
                                val currentGroup = groupChatId.trim()

                                // Step 1: Ensure both bot tokens are distinct
                                if (trimmedPartner.equals(trimmedAdmin, ignoreCase = false)) {
                                    withContext(Dispatchers.Main) {
                                        adminTokenApiError = "Tokens cannot be identical"
                                        partnerTokenApiError = "Tokens cannot be identical"
                                        validationError = "Partner Bot and Admin Bot must be two different bots. Please provide separate tokens from @BotFather."
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // Step 2: Live Verify Partner's Bot Token
                                val partnerRes = Validator.verifyBotToken(trimmedPartner)
                                when (partnerRes) {
                                    is Validator.ValidationResult.NetworkError -> {
                                        withContext(Dispatchers.Main) {
                                            networkErrorMessage = partnerRes.message
                                            showNetworkErrorDialog = true
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Error -> {
                                        withContext(Dispatchers.Main) {
                                            partnerTokenApiError = partnerRes.message
                                            validationError = "Partner Bot: ${partnerRes.message}"
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Success -> { /* Proceed */ }
                                }
                                val partnerUser = partnerRes.data
                                val partnerHandle = partnerUser.username?.let { "@$it" } ?: "Partner Bot"
                                withContext(Dispatchers.Main) { partnerBotHandle = partnerHandle }

                                // Step 3: Live Verify Admin's Bot Token
                                val adminRes = Validator.verifyBotToken(trimmedAdmin)
                                when (adminRes) {
                                    is Validator.ValidationResult.NetworkError -> {
                                        withContext(Dispatchers.Main) {
                                            networkErrorMessage = adminRes.message
                                            showNetworkErrorDialog = true
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Error -> {
                                        withContext(Dispatchers.Main) {
                                            adminTokenApiError = adminRes.message
                                            validationError = "Admin Bot: ${adminRes.message}\n(Partner Bot $partnerHandle token is valid ✓)"
                                            isLoading = false
                                        }
                                        return@launch
                                    }
                                    is Validator.ValidationResult.Success -> { /* Proceed */ }
                                }
                                val adminUser = adminRes.data
                                val adminHandle = adminUser.username?.let { "@$it" } ?: "Admin Bot"
                                withContext(Dispatchers.Main) { adminBotHandle = adminHandle }

                                // Step 4: Blind Spot Check - Bot ID Collision
                                if (partnerUser.id == adminUser.id) {
                                    withContext(Dispatchers.Main) {
                                        partnerTokenApiError = "Same bot account ($partnerHandle)"
                                        adminTokenApiError = "Same bot account ($adminHandle)"
                                        validationError = "Both tokens belong to the exact same bot account ($partnerHandle).\nYou must provide two separate bots created in @BotFather."
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // Step 5: Check Group Status for Both Bots Independently
                                val partnerGroupStatus = Validator.checkBotGroupMember(trimmedPartner, currentGroup, partnerUser)
                                val adminGroupStatus = Validator.checkBotGroupMember(trimmedAdmin, currentGroup, adminUser)

                                // Handle Network Errors
                                if (partnerGroupStatus is Validator.BotGroupMemberStatus.NetworkError || adminGroupStatus is Validator.BotGroupMemberStatus.NetworkError) {
                                    val netMsg = (partnerGroupStatus as? Validator.BotGroupMemberStatus.NetworkError)?.message
                                        ?: (adminGroupStatus as? Validator.BotGroupMemberStatus.NetworkError)?.message
                                        ?: "Network connection error"
                                    withContext(Dispatchers.Main) {
                                        networkErrorMessage = netMsg
                                        showNetworkErrorDialog = true
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // Handle Supergroup Migration Auto-Detection
                                if (partnerGroupStatus is Validator.BotGroupMemberStatus.Migrated || adminGroupStatus is Validator.BotGroupMemberStatus.Migrated) {
                                    val newId = (partnerGroupStatus as? Validator.BotGroupMemberStatus.Migrated)?.newChatId
                                        ?: (adminGroupStatus as? Validator.BotGroupMemberStatus.Migrated)!!.newChatId
                                    withContext(Dispatchers.Main) {
                                        groupChatId = newId
                                        groupApiError = "Group upgraded to Supergroup ($newId). Updated Chat ID."
                                        validationError = "The group was upgraded to a Supergroup ($newId). We have updated the Chat ID for you. Please tap 'Verify Credentials' again."
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // Handle Broadcast Channel vs Group
                                if (partnerGroupStatus is Validator.BotGroupMemberStatus.NotGroup || adminGroupStatus is Validator.BotGroupMemberStatus.NotGroup) {
                                    val type = (partnerGroupStatus as? Validator.BotGroupMemberStatus.NotGroup)?.actualType
                                        ?: (adminGroupStatus as? Validator.BotGroupMemberStatus.NotGroup)!!.actualType
                                    withContext(Dispatchers.Main) {
                                        groupApiError = "The entered ID is a $type, not a Group."
                                        validationError = "The entered Chat ID ($currentGroup) belongs to a $type. App-to-App mode requires a Private Group or Supergroup."
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // Comprehensive Cross-Bot Group Diagnostic Synthesis
                                val partnerSuccess = partnerGroupStatus is Validator.BotGroupMemberStatus.Success
                                val adminSuccess = adminGroupStatus is Validator.BotGroupMemberStatus.Success

                                if (!partnerSuccess || !adminSuccess) {
                                    withContext(Dispatchers.Main) {
                                        var errorBanner = ""

                                        if (partnerGroupStatus is Validator.BotGroupMemberStatus.NotInGroup && adminGroupStatus is Validator.BotGroupMemberStatus.NotInGroup) {
                                            partnerTokenApiError = "$partnerHandle is NOT in group"
                                            adminTokenApiError = "$adminHandle is NOT in group"
                                            groupApiError = "Neither bot is in the group"
                                            errorBanner = "Neither bot is in this group ($currentGroup).\n• Partner Bot ($partnerHandle): Missing ✗\n• Admin Bot ($adminHandle): Missing ✗\nPlease add both bots to the group and promote them to Administrators."
                                        } else if (partnerGroupStatus is Validator.BotGroupMemberStatus.NotInGroup && adminSuccess) {
                                            val title = (adminGroupStatus as Validator.BotGroupMemberStatus.Success).groupTitle
                                            partnerTokenApiError = "$partnerHandle is NOT in group '$title'"
                                            groupApiError = "Partner Bot missing from group"
                                            errorBanner = "Partner Bot ($partnerHandle) is NOT in group '$title'.\n(Admin Bot $adminHandle is verified in the group ✓).\nPlease add $partnerHandle to the group."
                                        } else if (adminGroupStatus is Validator.BotGroupMemberStatus.NotInGroup && partnerSuccess) {
                                            val title = (partnerGroupStatus as Validator.BotGroupMemberStatus.Success).groupTitle
                                            adminTokenApiError = "$adminHandle is NOT in group '$title'"
                                            groupApiError = "Admin Bot missing from group"
                                            errorBanner = "Admin Bot ($adminHandle) is NOT in group '$title'.\n(Partner Bot $partnerHandle is verified in the group ✓).\nPlease add $adminHandle to the group."
                                        } else if (partnerGroupStatus is Validator.BotGroupMemberStatus.NotAdmin) {
                                            partnerTokenApiError = "$partnerHandle is regular member (Not Admin)"
                                            groupApiError = "$partnerHandle needs Admin permissions"
                                            errorBanner = "Partner Bot ($partnerHandle) is in the group, but is NOT an Administrator.\n${if (adminSuccess) "(Admin Bot $adminHandle is verified as Admin ✓)\n" else ""}Please promote $partnerHandle to Administrator in Group Settings."
                                        } else if (adminGroupStatus is Validator.BotGroupMemberStatus.NotAdmin) {
                                            adminTokenApiError = "$adminHandle is regular member (Not Admin)"
                                            groupApiError = "$adminHandle needs Admin permissions"
                                            errorBanner = "Admin Bot ($adminHandle) is in the group, but is NOT an Administrator.\n${if (partnerSuccess) "(Partner Bot $partnerHandle is verified as Admin ✓)\n" else ""}Please promote $adminHandle to Administrator in Group Settings."
                                        } else if (partnerGroupStatus is Validator.BotGroupMemberStatus.Restricted) {
                                            partnerTokenApiError = "$partnerHandle is restricted"
                                            errorBanner = "Partner Bot ($partnerHandle) is restricted in the group. Please remove restrictions in Group Settings."
                                        } else if (adminGroupStatus is Validator.BotGroupMemberStatus.Restricted) {
                                            adminTokenApiError = "$adminHandle is restricted"
                                            errorBanner = "Admin Bot ($adminHandle) is restricted in the group. Please remove restrictions in Group Settings."
                                        } else if (partnerGroupStatus is Validator.BotGroupMemberStatus.GroupPrivacyEnabled) {
                                            partnerTokenApiError = "Group Privacy is ON for $partnerHandle"
                                            errorBanner = "Group Privacy is ENABLED for Partner Bot ($partnerHandle).\nIn @BotFather: select $partnerHandle -> Bot Settings -> Group Privacy -> Turn OFF."
                                        } else if (adminGroupStatus is Validator.BotGroupMemberStatus.GroupPrivacyEnabled) {
                                            adminTokenApiError = "Group Privacy is ON for $adminHandle"
                                            errorBanner = "Group Privacy is ENABLED for Admin Bot ($adminHandle).\nIn @BotFather: select $adminHandle -> Bot Settings -> Group Privacy -> Turn OFF."
                                        } else {
                                            val err1 = (partnerGroupStatus as? Validator.BotGroupMemberStatus.ApiError)?.message
                                            val err2 = (adminGroupStatus as? Validator.BotGroupMemberStatus.ApiError)?.message
                                            errorBanner = "Group verification error: ${err1 ?: err2 ?: "Unknown error"}"
                                            if (err1 != null) partnerTokenApiError = err1
                                            if (err2 != null) adminTokenApiError = err2
                                        }

                                        validationError = errorBanner
                                        isLoading = false
                                    }
                                    return@launch
                                }

                                // All verifications passed!
                                val finalGroupTitle = (partnerGroupStatus as Validator.BotGroupMemberStatus.Success).groupTitle
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    partnerTokenApiError = null
                                    adminTokenApiError = null
                                    groupApiError = null
                                    validationError = null
                                    extractedPartnerUsername = partnerHandle
                                    extractedAdminUsername = adminHandle
                                    verifiedGroupTitle = finalGroupTitle
                                    Config.adminAppToAppGroupTitle = finalGroupTitle
                                    Config.adminAppToAppPartnerBotUsername = partnerHandle
                                    Config.adminAppToAppMyBotUsername = adminHandle
                                    isVerified = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLight,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = SurfaceLevel2,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canVerify
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryLight, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Both Bots & Group...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (validationError != null) "Retry Verification" else "Verify Credentials",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Private • Encrypted • Hidden", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Admin Step 5: Preferences, Theme, Call Server & Flavor-Specific Disguise Settings.
 */
@Composable
fun AdminStep5Screen(onNext: () -> Unit) {
    val context = LocalContext.current

    var autoDownloadMedia by remember { mutableStateOf(Config.adminAutoDownloadMedia) }
    var newMessageNotification by remember { mutableStateOf(Config.adminNewMessageNotification) }
    var blockScreenshots by remember { mutableStateOf(Config.adminBlockScreenshots) }
    var callNotifications by remember { mutableStateOf(Config.adminCallNotifications) }
    var webrtcBaseUrl by remember { mutableStateOf(Config.adminCallServer) }
    var selectedTheme by remember { mutableStateOf(com.mobile.superiorsetup.theme.AppTheme.valueOf(Config.adminTheme)) }

    var customAccessWord by remember { mutableStateOf(Config.adminCustomAccessWord) }
    var customDialerCode by remember { mutableStateOf(Config.adminCustomDialerCode) }

    val isAccessWordValid = customAccessWord.isBlank() || customAccessWord.trim().length >= 4
    val isDialerCodeValid = customDialerCode.isBlank() || (customDialerCode.trim().length in 2..5 && customDialerCode.trim().all { it.isDigit() })
    val canContinue = isAccessWordValid && isDialerCodeValid

    var showWebRtcConfigPopup by remember { mutableStateOf(false) }
    var showDeveloperWarning by remember { mutableStateOf(false) }
    var showWebRtcInfo by remember { mutableStateOf(false) }
    var showFlavorInfo by remember { mutableStateOf(false) }
    var showAccessWordInfo by remember { mutableStateOf(false) }
    var showDialerCodeInfo by remember { mutableStateOf(false) }
    var showCallNotificationsInfo by remember { mutableStateOf(false) }

    if (showCallNotificationsInfo) {
        com.mobile.superiorsetup.ui.components.AdminCallNotificationsInfoDialog(onDismiss = { showCallNotificationsInfo = false })
    }

    if (showFlavorInfo) {
        com.mobile.superiorsetup.ui.components.AdminFlavorOptionsInfoDialog(onDismiss = { showFlavorInfo = false })
    }

    if (showAccessWordInfo) {
        com.mobile.superiorsetup.ui.components.AdminCustomAccessInfoDialog(onDismiss = { showAccessWordInfo = false })
    }

    if (showDialerCodeInfo) {
        com.mobile.superiorsetup.ui.components.AdminCustomDialerInfoDialog(onDismiss = { showDialerCodeInfo = false })
    }

    if (showDeveloperWarning) {
        com.mobile.superiorsetup.ui.components.AdminDeveloperWarningDialog(
            onConfirm = { showDeveloperWarning = false; showWebRtcConfigPopup = true },
            onDismiss = { showDeveloperWarning = false }
        )
    }

    if (showWebRtcInfo) {
        com.mobile.superiorsetup.ui.components.AdminCallConfigInfoDialog(onDismiss = { showWebRtcInfo = false })
    }

    if (showWebRtcConfigPopup) {
        com.mobile.superiorsetup.ui.components.WebRtcConfigPopup(
            initialUrl = webrtcBaseUrl,
            onDismiss = { showWebRtcConfigPopup = false },
            onSave = { newUrl ->
                webrtcBaseUrl = newUrl
                Config.adminCallServer = newUrl
                showWebRtcConfigPopup = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Admin Preferences", fontSize = 24.sp, color = TextPrimary)
            Text("Flavor & Chat Options", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Customize themes, media preferences, and flavor-specific disguise access codes.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // App Theme Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Theme", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.mobile.superiorsetup.theme.AppTheme.values().forEach { themeOpt ->
                        val isSelected = selectedTheme == themeOpt
                        val dotColor = themeOpt.primaryLightColor
                        val scale by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 1.15f else 1.0f)
                        val outlineAlpha by androidx.compose.animation.core.animateFloatAsState(if (isSelected) 0.5f else 0.0f)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .clickable {
                                    selectedTheme = themeOpt
                                    Config.adminTheme = themeOpt.name
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, dotColor.copy(alpha = outlineAlpha), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Background, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chat Preferences
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Chat Preferences", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSwitchRow(
                    title = "Auto Download Media",
                    subtitle = "Automatically download media and files",
                    icon = Icons.Default.Download,
                    iconTint = PrimaryLight,
                    isChecked = autoDownloadMedia,
                    onCheckedChange = {
                        autoDownloadMedia = it
                        Config.adminAutoDownloadMedia = it
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "New Message Notification",
                    subtitle = "Show notifications for new messages",
                    icon = Icons.Default.Notifications,
                    iconTint = PrimaryLight,
                    isChecked = newMessageNotification,
                    onCheckedChange = {
                        newMessageNotification = it
                        Config.adminNewMessageNotification = it
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Call Notifications",
                    subtitle = "Show alerts and notifications for incoming calls",
                    icon = Icons.Default.PhoneInTalk,
                    iconTint = PrimaryLight,
                    isChecked = callNotifications,
                    onCheckedChange = {
                        callNotifications = it
                        Config.adminCallNotifications = it
                    },
                    onInfoClick = { showCallNotificationsInfo = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Block Screenshots",
                    subtitle = "Prevent taking screenshots in chat",
                    icon = Icons.Default.VisibilityOff,
                    iconTint = PrimaryLight,
                    isChecked = blockScreenshots,
                    onCheckedChange = {
                        blockScreenshots = it
                        Config.adminBlockScreenshots = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call Configuration
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SettingsInputAntenna, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Call Configuration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp).clickable { showWebRtcInfo = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isDefault = webrtcBaseUrl.isEmpty()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .clickable { showDeveloperWarning = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Change Server", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text(if (isDefault) "Default Server" else "Custom Server", color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .clickable(enabled = !isDefault) {
                            if (!isDefault) {
                                webrtcBaseUrl = ""
                                Config.adminCallServer = ""
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Use Default", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("Reset to default server", color = TextSecondary, fontSize = 12.sp)
                    }
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isDefault) 1.05f else 1f,
                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
                        label = "switch_scale"
                    )

                    Switch(
                        modifier = Modifier.scale(scale),
                        checked = isDefault,
                        onCheckedChange = {
                            if (it && !isDefault) {
                                webrtcBaseUrl = ""
                                Config.adminCallServer = ""
                            }
                        },
                        thumbContent = if (isDefault) {
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
                        colors = com.mobile.superiorsetup.ui.components.luminaSwitchColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flavor Specific Options Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Flavor Specific Options", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp).clickable { showFlavorInfo = true }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Optional disguise launch codes. Only applied if the matching flavor is used on client side.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Weather: Custom Access Word
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Weather: Custom Access Word", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = "Info",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp).clickable { showAccessWordInfo = true }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Current: ", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (customAccessWord.isBlank()) "Superior Chat (Default)" else customAccessWord,
                                        color = PrimaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customAccessWord,
                        onValueChange = {
                            if (it.length <= 14) {
                                customAccessWord = it
                                Config.adminCustomAccessWord = it.trim()
                            }
                        },
                        placeholder = { Text("e.g. open door (Min 4 chars)", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        isError = !isAccessWordValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel1,
                            focusedContainerColor = SurfaceLevel1,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    if (!isAccessWordValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Minimum 4 characters required",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Captive Portal / Play Support: Custom Dialer Code
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Captive / Play: Dialer Code", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = "Info",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp).clickable { showDialerCodeInfo = true }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Current: ", color = TextSecondary, fontSize = 11.sp)
                                    Text(
                                        text = if (customDialerCode.isBlank()) "*#*#9131#*#* (Default)" else "*#*#$customDialerCode#*#*",
                                        color = PrimaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customDialerCode,
                        onValueChange = {
                            if (it.length <= 5 && it.all { char -> char.isDigit() }) {
                                customDialerCode = it
                                Config.adminCustomDialerCode = it.trim()
                            }
                        },
                        placeholder = { Text("e.g. 1234 (2 to 5 digits)", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        isError = !isDialerCodeValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel1,
                            focusedContainerColor = SurfaceLevel1,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    if (!isDialerCodeValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Minimum 2 digits required",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue to Setup QR Button
        Button(
            onClick = {
                if (canContinue) {
                    Config.adminAutoDownloadMedia = autoDownloadMedia
                    Config.adminNewMessageNotification = newMessageNotification
                    Config.adminCallNotifications = callNotifications
                    Config.adminBlockScreenshots = blockScreenshots
                    Config.adminCallServer = webrtcBaseUrl
                    Config.adminTheme = selectedTheme.name
                    Config.adminCustomAccessWord = customAccessWord.trim()
                    Config.adminCustomDialerCode = customDialerCode.trim()
                    onNext()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryLight,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = SurfaceLevel2,
                disabledContentColor = TextSecondary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Continue to Setup QR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Admin Step 6: Setup QR Generator & PIN Security Screen.
 */
@Composable
fun AdminStep6Screen() {
    val context = LocalContext.current

    var requirePin by remember { mutableStateOf(Config.adminRequirePin) }
    var showRequirePinInfo by remember { mutableStateOf(false) }
    var showDisablePinWarning by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var isGeneratingQr by remember { mutableStateOf(false) }

    var hardLockAdmin by remember { mutableStateOf(Config.adminHardLockAdmin) }
    var hardLockPartner by remember { mutableStateOf(Config.adminHardLockPartner) }
    var showHardLockInfo by remember { mutableStateOf(false) }
    var showPartnerWarning by remember { mutableStateOf(false) }
    var showAdminWarning by remember { mutableStateOf(false) }

    var lastGeneratedState by remember { mutableStateOf(Config.adminLastGeneratedState) }
    var currentPartnerQrPayload by remember {
        mutableStateOf(if (lastGeneratedState.isNotEmpty()) Config.adminLastEncryptedPayload else "")
    }
    var currentAdminQrPayload by remember {
        mutableStateOf(if (lastGeneratedState.isNotEmpty()) Config.adminLastAdminEncryptedPayload else "")
    }
    var currentQrPayload by remember { mutableStateOf("") }
    var qrDialogTitle by remember { mutableStateOf("Partner QR") }
    var qrDialogLabel by remember { mutableStateOf("Client QR (Direct DM)") }
    var currentPin by remember {
        mutableStateOf(if (lastGeneratedState.isNotEmpty()) Config.adminLastGeneratedPin else "")
    }

    val scope = rememberCoroutineScope()

    val generatePartnerJsonPayload = {
        val customWordJson = if (Config.adminCustomAccessWord.isNotBlank()) ""","customAccessWord":"${Config.adminCustomAccessWord}"""" else ""
        val customDialerJson = if (Config.adminCustomDialerCode.isNotBlank()) ""","customDialerCode":"${Config.adminCustomDialerCode}"""" else ""

        if (Config.adminChatMode == "APP_TO_APP") {
            """
                {
                    "role":"CLIENT",
                    "token":"${Config.adminAppToAppPartnerBotToken}",
                    "chatId":"${Config.adminAppToAppGroupId}",
                    "partnerUsername":"${Config.adminAppToAppMyBotUsername}",
                    "isPeerLinkEnabled":true,
                    "isAdminModeEnabled":false,
                    "isHardLocked":$hardLockPartner,
                    "autoDownloadMedia":${Config.adminAutoDownloadMedia},
                    "screenSecurity":${Config.adminBlockScreenshots},
                    "newMessageNotification":${Config.adminNewMessageNotification},
                    "callNotifications":${Config.adminCallNotifications},
                    "callServer":"${Config.adminCallServer}",
                    "theme":"${Config.adminTheme}"$customWordJson$customDialerJson
                }
            """.trimIndent().replace("\n", "").replace(" ", "")
        } else {
            """
                {
                    "role":"CLIENT",
                    "token":"${Config.adminTelegramBotToken}",
                    "chatId":"${Config.adminTelegramChatId}",
                    "isPeerLinkEnabled":false,
                    "isAdminModeEnabled":false,
                    "isHardLocked":$hardLockPartner,
                    "autoDownloadMedia":${Config.adminAutoDownloadMedia},
                    "screenSecurity":${Config.adminBlockScreenshots},
                    "newMessageNotification":${Config.adminNewMessageNotification},
                    "callNotifications":${Config.adminCallNotifications},
                    "callServer":"${Config.adminCallServer}",
                    "theme":"${Config.adminTheme}"$customWordJson$customDialerJson
                }
            """.trimIndent().replace("\n", "").replace(" ", "")
        }
    }

    val generateYourJsonPayload = {
        val customWordJson = if (Config.adminCustomAccessWord.isNotBlank()) ""","customAccessWord":"${Config.adminCustomAccessWord}"""" else ""
        val customDialerJson = if (Config.adminCustomDialerCode.isNotBlank()) ""","customDialerCode":"${Config.adminCustomDialerCode}"""" else ""

        if (Config.adminChatMode == "APP_TO_APP") {
            """
                {
                    "role":"ADMIN",
                    "isAdminModeEnabled":true,
                    "token":"${Config.adminAppToAppMyBotToken}",
                    "chatId":"${Config.adminAppToAppGroupId}",
                    "partnerUsername":"${Config.adminAppToAppPartnerBotUsername}",
                    "isPeerLinkEnabled":true,
                    "isHardLocked":$hardLockAdmin,
                    "autoDownloadMedia":${Config.adminAutoDownloadMedia},
                    "screenSecurity":${Config.adminBlockScreenshots},
                    "newMessageNotification":${Config.adminNewMessageNotification},
                    "callNotifications":${Config.adminCallNotifications},
                    "callServer":"${Config.adminCallServer}",
                    "theme":"${Config.adminTheme}"$customWordJson$customDialerJson
                }
            """.trimIndent().replace("\n", "").replace(" ", "")
        } else {
            """
                {
                    "role":"ADMIN",
                    "isAdminModeEnabled":true,
                    "token":"${Config.adminTelegramBotToken}",
                    "chatId":"${Config.adminTelegramChatId}",
                    "isHardLocked":$hardLockAdmin,
                    "autoDownloadMedia":${Config.adminAutoDownloadMedia},
                    "screenSecurity":${Config.adminBlockScreenshots},
                    "newMessageNotification":${Config.adminNewMessageNotification},
                    "callNotifications":${Config.adminCallNotifications},
                    "callServer":"${Config.adminCallServer}",
                    "theme":"${Config.adminTheme}"$customWordJson$customDialerJson
                }
            """.trimIndent().replace("\n", "").replace(" ", "")
        }
    }

    val currentStateHash = if (Config.adminChatMode == "APP_TO_APP") {
        "APP_TO_APP:${Config.adminAppToAppMyBotToken}:${Config.adminAppToAppPartnerBotToken}:${Config.adminAppToAppGroupId}:${Config.adminAppToAppMyBotUsername}:${Config.adminAppToAppPartnerBotUsername}:${Config.adminAutoDownloadMedia}:${Config.adminNewMessageNotification}:${Config.adminCallNotifications}:${Config.adminBlockScreenshots}:${Config.adminCallServer}:$requirePin:$hardLockAdmin:$hardLockPartner:${Config.adminTheme}:${Config.adminCustomAccessWord}:${Config.adminCustomDialerCode}"
    } else {
        "TELEGRAM:${Config.adminTelegramBotToken}:${Config.adminTelegramChatId}:${Config.adminTelegramChatName}:${Config.adminTelegramBotUsername}:${Config.adminAutoDownloadMedia}:${Config.adminNewMessageNotification}:${Config.adminCallNotifications}:${Config.adminBlockScreenshots}:${Config.adminCallServer}:$requirePin:$hardLockAdmin:$hardLockPartner:${Config.adminTheme}:${Config.adminCustomAccessWord}:${Config.adminCustomDialerCode}"
    }
    val isGenerated = lastGeneratedState.isNotEmpty() && lastGeneratedState == currentStateHash

    val showQrFor = { isPartner: Boolean ->
        if (!isGeneratingQr) {
            scope.launch {
                isGeneratingQr = true
                withContext(Dispatchers.Default) {
                    if (lastGeneratedState != currentStateHash || Config.adminLastEncryptedPayload.isEmpty() || (Config.adminChatMode == "APP_TO_APP" && Config.adminLastAdminEncryptedPayload.isEmpty())) {
                        val partnerRawJson = generatePartnerJsonPayload()
                        val yourRawJson = generateYourJsonPayload()

                        val newPin = if (requirePin) {
                            val secureRandom = java.security.SecureRandom()
                            String.format("%04d", secureRandom.nextInt(10000))
                        } else ""

                        val encryptedPartner = if (requirePin) Security.encryptAESWithPin(partnerRawJson, newPin) else Security.encryptAES(partnerRawJson)
                        val encryptedYour = if (requirePin) Security.encryptAESWithPin(yourRawJson, newPin) else Security.encryptAES(yourRawJson)

                        currentPartnerQrPayload = encryptedPartner
                        currentAdminQrPayload = encryptedYour
                        currentPin = newPin
                        lastGeneratedState = currentStateHash

                        Config.adminLastGeneratedState = currentStateHash
                        Config.adminLastGeneratedPin = newPin
                        Config.adminLastEncryptedPayload = encryptedPartner
                        Config.adminLastAdminEncryptedPayload = encryptedYour
                    } else {
                        currentPartnerQrPayload = Config.adminLastEncryptedPayload
                        currentAdminQrPayload = Config.adminLastAdminEncryptedPayload
                        currentPin = Config.adminLastGeneratedPin
                    }
                }
                isGeneratingQr = false
                currentQrPayload = if (isPartner) currentPartnerQrPayload else currentAdminQrPayload
                val modeSuffix = if (Config.adminChatMode == "APP_TO_APP") "(app-to-app)" else "(Direct DM)"
                val rolePrefix = if (isPartner) "Client QR" else "Admin QR"
                qrDialogTitle = if (isPartner) "Partner QR" else "Admin QR"
                qrDialogLabel = "$rolePrefix $modeSuffix"
                showQrDialog = true
            }
        }
    }

    if (showRequirePinInfo) {
        com.mobile.superiorsetup.ui.components.RequirePinInfoDialog(onDismiss = { showRequirePinInfo = false })
    }

    if (showHardLockInfo) {
        com.mobile.superiorsetup.ui.components.AdminHardLockInfoDialog(onDismiss = { showHardLockInfo = false })
    }

    if (showPartnerWarning) {
        com.mobile.superiorsetup.ui.components.AdminPartnerReadOnlyWarningDialog(
            onConfirm = {
                hardLockPartner = false
                Config.adminHardLockPartner = false
                showPartnerWarning = false
            },
            onDismiss = {
                showPartnerWarning = false
            }
        )
    }

    if (showAdminWarning) {
        com.mobile.superiorsetup.ui.components.AdminSelfReadOnlyWarningDialog(
            onConfirm = {
                hardLockAdmin = true
                Config.adminHardLockAdmin = true
                showAdminWarning = false
            },
            onDismiss = {
                showAdminWarning = false
            }
        )
    }

    if (showDisablePinWarning) {
        com.mobile.superiorsetup.ui.components.AdminDisablePinWarningDialog(
            onConfirm = {
                requirePin = false
                Config.adminRequirePin = false
                showDisablePinWarning = false
            },
            onDismiss = {
                showDisablePinWarning = false
            }
        )
    }

    if (showQrDialog) {
        com.mobile.superiorsetup.ui.components.DisplayQrPopup(
            payloadJson = currentQrPayload,
            pin = currentPin,
            title = qrDialogTitle,
            label = qrDialogLabel,
            onDismiss = { showQrDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Admin Setup", fontSize = 24.sp, color = TextPrimary)
            Text("Generate Setup QR", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (Config.adminChatMode == "APP_TO_APP")
                    "Your setup configuration is ready. Generate encrypted QR codes for your partner or for your own device."
                else
                    "Your setup configuration is ready. Generate the encrypted setup QR code for your partner's app.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Configuration Summary Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Configuration Summary", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceLevel2)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• Mode: ${if (Config.adminChatMode == "APP_TO_APP") "App-to-App (PeerLink)" else "Telegram Direct (1-on-1)"}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (Config.adminChatMode == "APP_TO_APP") {
                        val groupTitle = Config.adminAppToAppGroupTitle
                        val groupLabel = if (groupTitle.isNotEmpty()) " ($groupTitle)" else ""
                        Text(
                            text = "• Chat Target: ${Config.adminAppToAppGroupId}$groupLabel",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        if (Config.adminAppToAppPartnerBotUsername.isNotEmpty()) {
                            Text(
                                text = "• Partner Bot: ${Config.adminAppToAppPartnerBotUsername}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (Config.adminAppToAppMyBotUsername.isNotEmpty()) {
                            Text(
                                text = "• Your Bot: ${Config.adminAppToAppMyBotUsername}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        val chatName = Config.adminTelegramChatName
                        val chatLabel = if (chatName.isNotEmpty()) " ($chatName)" else ""
                        Text(
                            text = "• Chat Target: ${Config.adminTelegramChatId}$chatLabel",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        if (Config.adminTelegramBotUsername.isNotEmpty()) {
                            Text(
                                text = "• Bot: ${Config.adminTelegramBotUsername}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "• Theme: ${Config.adminTheme}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (Config.adminCustomAccessWord.isNotEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextSecondary)) {
                                    append("• Custom Access Word: ")
                                }
                                withStyle(SpanStyle(color = PrimaryLight, fontWeight = FontWeight.SemiBold)) {
                                    append(Config.adminCustomAccessWord)
                                }
                            },
                            fontSize = 12.sp
                        )
                    }
                    if (Config.adminCustomDialerCode.isNotEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TextSecondary)) {
                                    append("• Custom Dialer Code: ")
                                }
                                withStyle(SpanStyle(color = PrimaryLight, fontWeight = FontWeight.SemiBold)) {
                                    append("*#*#${Config.adminCustomDialerCode}#*#*")
                                }
                            },
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextSecondary)) {
                                append("• Partner Admin Lock: ")
                            }
                            withStyle(SpanStyle(color = if (hardLockPartner) PrimaryLight else ErrorRed, fontWeight = FontWeight.SemiBold)) {
                                append(if (hardLockPartner) "Hard Locked" else "Unlocked")
                            }
                        },
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hard Lock Admin Settings Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Hard Lock Admin Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp).clickable { showHardLockInfo = true }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (Config.adminChatMode == "APP_TO_APP") {
                    // Switch 1: Hard Lock for Your QR
                    SettingsSwitchRow(
                        title = "Hard Lock for Your QR",
                        subtitle = "Disables Read only toggle for Admin side, Keep settings untouch",
                        icon = Icons.Filled.Lock,
                        iconTint = PrimaryLight,
                        isChecked = hardLockAdmin,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                showAdminWarning = true
                            } else {
                                hardLockAdmin = false
                                Config.adminHardLockAdmin = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch 2: Hard Lock for Partner QR
                    SettingsSwitchRow(
                        title = "Hard Lock for Partner QR",
                        subtitle = "Disables Read only toggle for Partner side, Keep settings untouch",
                        icon = Icons.Filled.Security,
                        iconTint = PrimaryLight,
                        isChecked = hardLockPartner,
                        onCheckedChange = { newValue ->
                            if (!newValue) {
                                showPartnerWarning = true
                            } else {
                                hardLockPartner = true
                                Config.adminHardLockPartner = true
                            }
                        }
                    )
                } else {
                    // Telegram Mode: Single switch for Partner
                    SettingsSwitchRow(
                        title = "Hard Lock for Partner QR",
                        subtitle = "Disables Read only toggle for Partner side, Keep settings untouch",
                        icon = Icons.Filled.Shield,
                        iconTint = PrimaryLight,
                        isChecked = hardLockPartner,
                        onCheckedChange = { newValue ->
                            if (!newValue) {
                                showPartnerWarning = true
                            } else {
                                hardLockPartner = true
                                Config.adminHardLockPartner = true
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Setup QR Generation Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Security & PIN Protection", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSwitchRow(
                    title = "Require PIN",
                    subtitle = if (requirePin) "Enabled: Scanning QR requires a 4-digit PIN" else "Disabled: QR can be decrypted directly",
                    icon = Icons.Default.Lock,
                    iconTint = PrimaryLight,
                    isChecked = requirePin,
                    onCheckedChange = { newValue ->
                        if (!newValue) {
                            showDisablePinWarning = true
                        } else {
                            requirePin = true
                            Config.adminRequirePin = true
                        }
                    },
                    onInfoClick = { showRequirePinInfo = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (Config.adminChatMode == "APP_TO_APP") {
                    // Horizontal Buttons: Left "Your QR" (neutral), Right "Partner's QR" (PrimaryLight + glow)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Button: "Your QR" (without primary light and blur)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .bounceClick(scaleDown = 0.95f) {
                                    showQrFor(false)
                                }
                                .background(SurfaceLevel2, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGeneratingQr) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Your QR", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Right Button: "Partner's QR" (with primary light and blur/glow)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .bounceClick(scaleDown = 0.95f) {
                                    showQrFor(true)
                                }
                                .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 8f, cornerRadius = 16.dp)
                                .background(PrimaryLight, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGeneratingQr) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Partner's QR", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Telegram Mode: Single Primary Button
                    val singleButtonText = if (!isGenerated) "Generate QR for Partner" else "Display QR"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                showQrFor(true)
                            }
                            .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 8f, cornerRadius = 16.dp)
                            .background(PrimaryLight, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGeneratingQr) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(singleButtonText, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Tag (at the end of the Security card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = if (isGenerated) Success.copy(alpha = 0.12f) else TextSecondary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isGenerated) Success.copy(alpha = 0.35f) else TextSecondary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isGenerated) Success else TextSecondary.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGenerated) "Generated • Click to Display" else "Not Generated Yet",
                                color = if (isGenerated) Success else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isGenerated) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encryption Footer (outside the card)
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (requirePin) "Encrypted with AES-GCM & 4-digit PIN." else "Encrypted directly with AES-GCM.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = TextPrimary,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isChecked) 1.05f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "switch_scale"
    )
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
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
                .alpha(animatedAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
            }
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
                                .size(16.dp)
                                .clickable { onInfoClick() }
                        )
                    }
                }
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
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
            colors = com.mobile.superiorsetup.ui.components.luminaSwitchColors()
        )
    }
}
