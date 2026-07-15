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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                            text = if (currentMode == SetupMode.CLIENT) "Step $currentStep of 3" else "Step $currentStep of 2",
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
                    2 -> if (mode == SetupMode.CLIENT) Step2Screen(onNext = { currentStep = 3 }) else AdminStep2Screen()
                    3 -> Step3Screen()
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
                                color = if (currentMode == SetupMode.CLIENT) Primary.copy(alpha = 0.15f) else SurfaceLevel2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (currentMode == SetupMode.CLIENT) Primary.copy(alpha = 0.5f) else DividerColor),
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
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = if (currentMode == SetupMode.CLIENT) Primary else TextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Client Mode", color = if (currentMode == SetupMode.CLIENT) Primary else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Surface(
                                color = if (currentMode == SetupMode.ADMIN) Primary.copy(alpha = 0.15f) else SurfaceLevel2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (currentMode == SetupMode.ADMIN) Primary.copy(alpha = 0.5f) else DividerColor),
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
                                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = if (currentMode == SetupMode.ADMIN) Primary else TextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Admin Mode", color = if (currentMode == SetupMode.ADMIN) Primary else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
    var isInstalled by remember { mutableStateOf(AppManager.isAppInstalled(context, "com.mobile.superiorutils")) }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isInstalled = AppManager.isAppInstalled(context, "com.mobile.superiorutils")
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
            Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = Primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Welcome to", fontSize = 24.sp, color = TextPrimary)
            Text("Superior Chat", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
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
                    Button(
                        onClick = { onNext() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else if (isInstalled) {
                    Button(
                        onClick = { onNext() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Installed - Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                showInstallDialog = true
                            } else {
                                AppManager.installApp(context) { intent -> installLauncher.launch(intent) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install Main Application", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
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
            onSuccess = { token, chat ->
                Config.botToken = token
                Config.chatId = chat
                botToken = token
                chatId = chat
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter Credentials", fontSize = 24.sp, color = TextPrimary)
            Text("Bot Token & Chat ID", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Connect your Telegram bot to enable secure messaging.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FeatureRow(Icons.Filled.SmartToy, "@BotFather", "Create a new bot on Telegram and copy your Bot Token.")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.PersonSearch, "@MissRose_bot", "Search MissRose_bot and use /id command to get your Chat ID.")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { showAddManuallyPopup = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (botToken.isNotEmpty()) SurfaceLevel1 else Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (botToken.isNotEmpty()) Icons.Filled.Edit else Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (botToken.isNotEmpty()) "Edit Manually" else "Add Manually", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showScanner = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR Code", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            
            Button(
                onClick = { onNext() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = botToken.isNotBlank() && chatId.isNotBlank()
            ) {
                Text("Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel1)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Credentials are stored securely on device", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AdminStep2Screen() {
    var botToken by remember { mutableStateOf(Config.adminBotToken) }
    var chatId by remember { mutableStateOf(Config.adminChatId) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        com.mobile.superiorsetup.ui.components.AddCredentialsPopup(
            initialToken = botToken,
            initialChatId = chatId,
            isReplace = botToken.isNotEmpty() && chatId.isNotEmpty(),
            onDismiss = { showAddDialog = false },
            onGenerate = { token, chat ->
                botToken = token
                chatId = chat
                Config.adminBotToken = token
                Config.adminChatId = chat
                showAddDialog = false
                showQrDialog = true // Flow directly into QR display after generating
            }
        )
    }
    
    // Show QR Dialog
    if (showQrDialog) {
        com.mobile.superiorsetup.ui.components.DisplayQrPopup(
            botToken = botToken,
            chatId = chatId,
            onDismiss = { showQrDialog = false }
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
            Icon(Icons.Filled.QrCode, contentDescription = null, tint = Primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("QR Generation", fontSize = 24.sp, color = TextPrimary)
            Text("Encrypted Payload", fontSize = 24.sp, color = Primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Generate an encrypted configuration QR code for the host device to scan seamlessly.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FeatureRow(Icons.Filled.SmartToy, "Bot Token", "Get this from @BotFather on Telegram by sending the /newbot command.")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureRow(Icons.Filled.PersonSearch, "Chat ID", "Get this from @MissRose_bot by sending the /id command.")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (botToken.isNotEmpty()) SurfaceLevel1 else Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (botToken.isNotEmpty()) Icons.Filled.Edit else Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (botToken.isNotEmpty()) "Replace Credentials" else "Add Credentials", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            
            if (botToken.isNotEmpty() && chatId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Display QR", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel1)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("QR payload will be securely encrypted with AES-GCM.", color = TextSecondary, fontSize = 11.sp)
            }
        }
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
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(64.dp))
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
            Button(
                onClick = {
                    AppManager.wakeUpMainApp(context, Config.botToken, Config.chatId)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open Main Application", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
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
            Text(title, color = Primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
            Text(title, color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(status, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
    }
}



