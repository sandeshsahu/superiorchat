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

enum class SetupMode { CLIENT, ADMIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupUI() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Config.init(context.applicationContext)
    }

    var currentStep by remember { mutableIntStateOf(if (Config.botToken.isNotEmpty() && Config.chatId.isNotEmpty()) 3 else 1) }
    var currentMode by remember { mutableStateOf(SetupMode.CLIENT) }
    var modeDropdownExpanded by remember { mutableStateOf(false) }
    
    BackHandler(enabled = currentStep > 1) {
        currentStep--
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    Surface(
                        color = SurfaceLevel1,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (currentMode == SetupMode.CLIENT) "Step $currentStep of 3" else "Step $currentStep of 3",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = Pair(currentStep, currentMode),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f))
                },
                label = "step_transition",
                modifier = Modifier.fillMaxSize()
            ) { (step, mode) ->
                when (step) {
                    1 -> Step1Screen(mode = mode, onNext = { currentStep = 2 })
                    2 -> if (mode == SetupMode.CLIENT) Step2Screen(onNext = { currentStep = 3 }) else AdminStep2Screen(onNext = { currentStep = 3 })
                    3 -> if (mode == SetupMode.CLIENT) Step3Screen() else AdminStep3Screen()
                }
            }
            
            // Mode Switcher Dropdown
            if (currentStep == 1) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp)) {
                    Surface(
                        color = SurfaceLevel1,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { modeDropdownExpanded = true }
                    ) {
                        Text(
                            text = if (currentMode == SetupMode.CLIENT) "Client Mode" else "Admin Mode",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = modeDropdownExpanded,
                        onDismissRequest = { modeDropdownExpanded = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Background)
                            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp).width(160.dp)) {
                            Surface(
                                color = if (currentMode == SetupMode.CLIENT) PrimaryLight.copy(alpha = 0.15f) else SurfaceLevel2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (currentMode == SetupMode.CLIENT) PrimaryLight.copy(alpha = 0.5f) else DividerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        currentMode = SetupMode.CLIENT
                                        modeDropdownExpanded = false 
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = if (currentMode == SetupMode.CLIENT) PrimaryLight else TextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Client Mode", color = if (currentMode == SetupMode.CLIENT) PrimaryLight else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Surface(
                                color = if (currentMode == SetupMode.ADMIN) PrimaryLight.copy(alpha = 0.15f) else SurfaceLevel2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (currentMode == SetupMode.ADMIN) PrimaryLight.copy(alpha = 0.5f) else DividerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        currentMode = SetupMode.ADMIN
                                        modeDropdownExpanded = false 
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = if (currentMode == SetupMode.ADMIN) PrimaryLight else TextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Admin Mode", color = if (currentMode == SetupMode.ADMIN) PrimaryLight else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Step1Screen(mode: SetupMode, onNext: () -> Unit) {
    val context = LocalContext.current
    var isInstalled by remember { mutableStateOf(AppManager.isAppInstalled(context, com.mobile.superiorsetup.BuildConfig.TARGET_APP_ID)) }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isInstalled = AppManager.isAppInstalled(context, com.mobile.superiorsetup.BuildConfig.TARGET_APP_ID)
        if (isInstalled) {
            onNext()
        }
    }

    // Permission launcher for REQUEST_INSTALL_PACKAGES if needed on older devices / specific OS variants
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Continue to install regardless, might have granted it.
        AppManager.installApp(context) { intent -> installLauncher.launch(intent) }
    }
    
    var showInstallDialog by remember { mutableStateOf(false) }

    if (showInstallDialog) {
        com.mobile.superiorsetup.ui.components.ActionDialog(
            title = "Installation Permission Required",
            message = "To install the main app, you need to allow the Setup App to install unknown apps.",
            confirmText = "Settings",
            dismissText = "Not Now",
            onConfirm = {
                showInstallDialog = false
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                permissionLauncher.launch(intent)
            },
            onDismiss = { showInstallDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Chat, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Welcome to", fontSize = 24.sp, color = TextPrimary)
            Text("Superior Chat", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "A hidden and secure chat experience powered by Telegram Bot API.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FeatureRow(Icons.Filled.Shield, "Fully Private", "Your chats stay hidden. Access the app only via a secret dialer code.")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.FlashOn, "Telegram Powered", "Reliable messaging with Telegram Bot API for fast and secure delivery.")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.VisibilityOff, "Invisible Mode", "The app is completely hidden from your app drawer and recent apps list.")
        }
        
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f))
            },
            label = "button_mode_animation",
            modifier = Modifier.fillMaxWidth()
        ) { currentModeState ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (currentModeState == SetupMode.ADMIN) {
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
                            Text("Continue", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        }
                    }
                } else if (isInstalled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bounceClick(scaleDown = 0.95f) { onNext() }
                            .glow(color = Success, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                            .background(Success, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("App Installed - Continue", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                    showInstallDialog = true
                                } else {
                                    AppManager.installApp(context) { intent -> installLauncher.launch(intent) }
                                }
                            }
                            .glow(color = PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                            .background(PrimaryLight, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Main Application", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Private • Encrypted • Hidden", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

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
fun AdminStep2Screen(onNext: () -> Unit) {
    var botToken by remember { mutableStateOf(Config.adminBotToken) }
    var chatId by remember { mutableStateOf(Config.adminChatId) }
    var tokenVisible by remember { mutableStateOf(false) }

    val isTokenValid by remember(botToken) { derivedStateOf { botToken.isBlank() || com.mobile.superiorsetup.core.Validator.isValidBotToken(botToken.trim()) } }
    val isChatIdValid by remember(chatId) { derivedStateOf { chatId.isBlank() || com.mobile.superiorsetup.core.Validator.isValidChatId(chatId.trim()) } }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.VpnKey, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Admin Credentials", fontSize = 24.sp, color = TextPrimary)
            Text("Bot Token & Chat ID", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter the Bot Token and Chat ID to configure the host device securely.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bot Token Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isTokenValid) ErrorRed else DividerColor),
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
                        onValueChange = { botToken = it },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isTokenValid,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chat ID Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isChatIdValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("e.g. 1234567890", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isChatIdValid,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FeatureRow(Icons.Filled.Info, "How to get these?", "• Bot Token: Chat with @BotFather on Telegram\n• Chat ID: Chat with @userinfobot to get your ID")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val isReady = botToken.isNotBlank() && chatId.isNotBlank()
            val isValid = isReady && isTokenValid && isChatIdValid
            val isError = isReady && !isValid
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = if (isValid) 0.95f else 1f) {
                        if (isValid) {
                            Config.adminBotToken = botToken.trim()
                            Config.adminChatId = chatId.trim()
                            onNext()
                        }
                    }
                    .glow(color = if (isValid) PrimaryLight else Color.Transparent, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(if (isError) ErrorRed else if (isValid) PrimaryLight else SurfaceLevel2, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isError) "Invalid Credentials" else if (isValid) "Save & Continue" else "Continue",
                    color = if (isError) Color.White else if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Private • Encrypted • Hidden", color = TextSecondary, fontSize = 11.sp)
                }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun AdminStep3Screen() {
    val context = LocalContext.current
    
    var autoDownloadMedia by remember { mutableStateOf(Config.adminAutoDownloadMedia) }
    var newMessageNotification by remember { mutableStateOf(Config.adminNewMessageNotification) }
    var blockScreenshots by remember { mutableStateOf(Config.adminBlockScreenshots) }
    var webrtcBaseUrl by remember { mutableStateOf(Config.adminCallServer) }
    
    var showWebRtcConfigPopup by remember { mutableStateOf(false) }
    var showDeveloperWarning by remember { mutableStateOf(false) }
    
    var showQrDialog by remember { mutableStateOf(false) }
    var currentQrPayload by remember { mutableStateOf("") }
    var lastGeneratedState by remember { mutableStateOf("") }

    var isCheckingUrl by remember { mutableStateOf(false) }
    var showNetworkError by remember { mutableStateOf(false) }
    var showWebRtcInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentStateHash = "${Config.adminBotToken}:${Config.adminChatId}:$autoDownloadMedia:$newMessageNotification:$blockScreenshots:$webrtcBaseUrl"
    
    if (showNetworkError) {
        com.mobile.superiorsetup.ui.components.ActionDialog(
            title = "Server Unreachable",
            message = "Could not verify *call.html* on the provided server. Please check the URL and ensure the server is accessible.",
            icon = Icons.Filled.Warning,
            iconTint = ErrorRed,
            confirmText = "Okay",
            dismissText = "",
            onConfirm = { showNetworkError = false },
            onDismiss = { showNetworkError = false }
        )
    }
    
    if (showQrDialog) {
        com.mobile.superiorsetup.ui.components.DisplayQrPopup(
            payloadJson = currentQrPayload,
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
            Icon(Icons.Filled.Settings, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Admin Settings", fontSize = 24.sp, color = TextPrimary)
            Text("Configuration", fontSize = 24.sp, color = PrimaryLight, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Customize settings and generate the encrypted QR code for the host device.",
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
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Chat Preferences", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceLevel1)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SettingsInputAntenna, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Call Configuration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Icon(
                        Icons.Filled.Info, 
                        contentDescription = "Info", 
                        tint = TextSecondary, 
                        modifier = Modifier.padding(4.dp).size(20.dp).clickable { showWebRtcInfo = true }
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
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        if (showDeveloperWarning) {
            com.mobile.superiorsetup.ui.components.ActionDialog(
                title = "Developer Setting",
                message = "This setting is strictly for *Developers*!\nChanging the *Server URL* can permanently *Break* the Calling feature. If you are not a developer, please *Cancel* this.",
                icon = Icons.Filled.Warning,
                iconTint = ErrorRed,
                confirmText = "I Understand",
                dismissText = "Cancel",
                onConfirm = { showDeveloperWarning = false; showWebRtcConfigPopup = true },
                onDismiss = { showDeveloperWarning = false }
            )
        }
        
        if (showWebRtcInfo) {
            com.mobile.superiorsetup.ui.components.ActionDialog(
                title = "Call Configuration",
                message = "You can configure your custom *WebRTC Server* URL for voice calls, or reset it to the default server if you experience connection issues.\n\nCheck developer's *Github Page* for more information",
                icon = Icons.Filled.Info,
                iconTint = PrimaryLight,
                confirmText = "Okay",
                dismissText = "",
                onConfirm = { showWebRtcInfo = false },
                onDismiss = { showWebRtcInfo = false }
            )
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
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val isButtonReady = lastGeneratedState.isNotEmpty() && lastGeneratedState == currentStateHash
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        val json = """
                            {
                                "token":"${Config.adminBotToken}",
                                "chatId":"${Config.adminChatId}",
                                "autoDownloadMedia":$autoDownloadMedia,
                                "screenSecurity":$blockScreenshots,
                                "newMessageNotification":$newMessageNotification,
                                "callServer":"$webrtcBaseUrl"
                            }
                        """.trimIndent().replace("\\n", "").replace(" ", "")
                        
                        currentQrPayload = json
                        lastGeneratedState = currentStateHash
                        showQrDialog = true
                    }
                    .glow(color = if (isButtonReady) Success else PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(if (isButtonReady) Success else PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = if (isButtonReady) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    val btnText = if (lastGeneratedState.isEmpty()) "Generate QR" 
                                  else if (lastGeneratedState == currentStateHash) "Display QR" 
                                  else "Regenerate QR"
                    Text(btnText, color = if (isButtonReady) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("QR payload will be securely encrypted with AES-GCM.", color = TextSecondary, fontSize = 11.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
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
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isChecked) 1.05f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "switch_scale"
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
            colors = com.mobile.superiorsetup.ui.components.luminaSwitchColors()
        )
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

@Composable
fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun StatusRow(icon: ImageVector, title: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(status, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
    }
}



