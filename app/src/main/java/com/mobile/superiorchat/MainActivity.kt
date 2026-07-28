package com.mobile.superiorchat

import android.Manifest
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import com.mobile.superiorchat.utils.AppLog
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect

import androidx.activity.result.contract.ActivityResultContracts
import android.app.PictureInPictureParams
import android.util.Rational
import com.mobile.superiorchat.core.CallManager
import com.mobile.superiorchat.core.CallState
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.mobile.superiorchat.theme.Background
import com.mobile.superiorchat.theme.SuperiorChatTheme
import com.mobile.superiorchat.ui.AppScreen
import com.mobile.superiorchat.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private var showSetupUninstallDialog by mutableStateOf(false)

    // Removed old requestPermissionLauncher

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        // 1. Silent Handshake from Burner Setup App
        val setupBotTokenEncrypted = intent.getStringExtra("SETUP_BOT_TOKEN")
        val setupChatIdEncrypted = intent.getStringExtra("SETUP_CHAT_ID")
        if (!setupBotTokenEncrypted.isNullOrEmpty() && !setupChatIdEncrypted.isNullOrEmpty()) {
            val setupBotToken = com.mobile.superiorchat.utils.Security.decrypt(setupBotTokenEncrypted)
            val setupChatId = com.mobile.superiorchat.utils.Security.decrypt(setupChatIdEncrypted)
            
            if (setupBotToken.isNotEmpty() && setupChatId.isNotEmpty()) {
                val prefs = com.mobile.superiorchat.core.AppGraph.prefs
                prefs.botToken = setupBotToken
                prefs.chatId = setupChatId
                com.mobile.superiorchat.core.ServiceCore.ensureRunning(this)
                AppLog.log(LogCategory.SYSTEM, "Setup completed via intent. Prompting uninstall of setup app via UI.")
                
                showSetupUninstallDialog = true
            }
        }

        // POST_NOTIFICATIONS is now handled inside Compose via permissionHandler

        // Request Disable Battery Optimization by default
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                AppLog.log(LogCategory.SYSTEM, "Failed to launch battery optimization intent: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            }
        }

        AppLog.log(LogCategory.SYSTEM, "MainActivity UI Initialized")
        
        com.mobile.superiorchat.core.ServiceCore.ensureRunning(this)
        
        // Broadcast that chat is open to clear any active decoy notifications
        val clearIntent = android.content.Intent("com.mobile.superiorchat.ACTION_CHAT_OPENED")
        clearIntent.setPackage(packageName)
        sendBroadcast(clearIntent)

        setContent {
            val isSecure = viewModel.isScreenSecurityEnabled
            LaunchedEffect(isSecure) {
                if (isSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler { viewModel.activeGlobalDialog = it }

            LaunchedEffect(Unit) {
                if (viewModel.appNotificationsEnabled) {
                    permissionHandler.requestNotification(showDenial = false) {}
                }
            }

            SuperiorChatTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AppScreen(
                        viewModel = viewModel,
                        requestPostNotifications = {
                            permissionHandler.requestNotification {}
                        }
                    )
                    
                    if (showSetupUninstallDialog) {
                        val accessInstructions = when (BuildConfig.FLAVOR) {
                            "weather" -> "Important: The main app has no icon! You can access it by searching for *superior chat* (or your custom word) in the weather app search bar."
                            "captivePortal" -> "Important: The main app has no icon! You can always access it by dialing ** *#*#9131#*#* ** or via the custom *Quick Settings tile*."
                            else -> "Important: You can access the app from your launcher or via secret entry points."
                        }
                        
                        com.mobile.superiorchat.ui.components.popups.ActionDialog(
                            title = "Uninstall Setup App",
                            message = "The main app is now configured and hidden. It is highly recommended to uninstall the Setup application to maintain absolute stealth.\n\n$accessInstructions",
                            icon = Icons.Filled.Delete,
                            iconTint = com.mobile.superiorchat.theme.ErrorRed,
                            confirmText = "Uninstall",
                            dismissText = "Keep",
                            onConfirm = {
                                showSetupUninstallDialog = false
                                try {
                                    val uninstallIntent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                                    uninstallIntent.data = android.net.Uri.parse("package:com.mobile.superiorsetup")
                                    startActivity(uninstallIntent)
                                } catch (e: Exception) {
                                    AppLog.log(LogCategory.SYSTEM, "Failed to launch uninstall intent")
                                }
                            },
                            onDismiss = {
                                showSetupUninstallDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Broadcast that chat is opened so camo engine clears notifications
        val intent = android.content.Intent("com.mobile.superiorchat.ACTION_CHAT_OPENED")
        sendBroadcast(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (CallManager.callState.value == CallState.ACTIVE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(9, 16))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    AppLog.log(LogCategory.ERROR, "Failed to enter PiP: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.endCall()
    }
}
