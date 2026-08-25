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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.result.contract.ActivityResultContracts
import android.app.PictureInPictureParams
import android.util.Rational
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.mobile.superiorchat.ui.components.popups.*
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

open class MainActivity : ComponentActivity() {
    private var showSetupUninstallDialog by mutableStateOf(false)
    var isInPipMode by mutableStateOf(false)
        private set
    private var wasInPipMode = false

    private val viewModel: MainViewModel by viewModels()

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                CallManager.endCall()
                viewModel.lockApp()
                finishAndRemoveTask()
            }
        }
    }

    private val pipActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.mobile.superiorchat.ACTION_END_CALL") {
                CallManager.endCall()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenOffReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenOffReceiver, filter)
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to register screenOffReceiver: ${e.message}", LogLevel.WARN)
        }

        val pipFilter = android.content.IntentFilter("com.mobile.superiorchat.ACTION_END_CALL")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pipActionReceiver, pipFilter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(pipActionReceiver, pipFilter)
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to register pipActionReceiver: ${e.message}", LogLevel.WARN)
        }

        if (com.mobile.superiorchat.core.AppGraph.prefs.isFakeCrashEnabled) {
            setTheme(R.style.Theme_SuperiorChat_Transparent)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        // 1. Silent Handshake from Burner Setup App
        val setupBotTokenEncrypted = intent.getStringExtra("SETUP_BOT_TOKEN")
        val setupChatIdEncrypted = intent.getStringExtra("SETUP_CHAT_ID")
        val autoDownloadEnc = intent.getStringExtra("SETUP_AUTO_DOWNLOAD")
        val screenSecurityEnc = intent.getStringExtra("SETUP_BLOCK_SCREENSHOTS")
        val notificationsEnc = intent.getStringExtra("SETUP_NOTIFICATIONS")
        val callServerEnc = intent.getStringExtra("SETUP_CALL_SERVER")
        val themeEnc = intent.getStringExtra("SETUP_THEME")

        if (!setupBotTokenEncrypted.isNullOrEmpty() && !setupChatIdEncrypted.isNullOrEmpty()) {
            val setupBotToken = com.mobile.superiorchat.utils.Security.decrypt(setupBotTokenEncrypted)
            val setupChatId = com.mobile.superiorchat.utils.Security.decrypt(setupChatIdEncrypted)
            
            if (setupBotToken.isNotEmpty() && setupChatId.isNotEmpty()) {
                val prefs = com.mobile.superiorchat.core.AppGraph.prefs
                prefs.botToken = setupBotToken
                prefs.chatId = setupChatId

                if (!autoDownloadEnc.isNullOrEmpty()) {
                    prefs.isAutoDownloadMediaEnabled = com.mobile.superiorchat.utils.Security.decrypt(autoDownloadEnc).toBoolean()
                }
                if (!screenSecurityEnc.isNullOrEmpty()) {
                    prefs.isScreenSecurityEnabled = com.mobile.superiorchat.utils.Security.decrypt(screenSecurityEnc).toBoolean()
                }
                if (!notificationsEnc.isNullOrEmpty()) {
                    prefs.isNewMessageNotificationEnabled = com.mobile.superiorchat.utils.Security.decrypt(notificationsEnc).toBoolean()
                }
                if (!callServerEnc.isNullOrEmpty()) {
                    val server = com.mobile.superiorchat.utils.Security.decrypt(callServerEnc)
                    if (server.isNotEmpty()) prefs.webrtcBaseUrl = server
                }
                if (!themeEnc.isNullOrEmpty()) {
                    val setupTheme = com.mobile.superiorchat.utils.Security.decrypt(themeEnc)
                    if (setupTheme.isNotEmpty()) prefs.appTheme = setupTheme
                }

                com.mobile.superiorchat.core.ServiceCore.ensureRunning(this)
                AppLog.log(LogCategory.SYSTEM, "Setup completed via intent. Prompting uninstall of setup app via UI.")
                
                showSetupUninstallDialog = true
            }
        }

        // POST_NOTIFICATIONS is now handled inside Compose via permissionHandler

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

            val isExcludeFromRecents = viewModel.isExcludeFromRecentsEnabled
            LaunchedEffect(isExcludeFromRecents) {
                updateExcludeFromRecents(isExcludeFromRecents)
            }

            val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler { viewModel.activeGlobalDialog = it }
            var showTerms by remember { mutableStateOf(!com.mobile.superiorchat.core.AppGraph.prefs.hasAgreedToTerms) }

            LaunchedEffect(showTerms, showSetupUninstallDialog) {
                if (!showTerms && !showSetupUninstallDialog) {
                    val context = this@MainActivity
                    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.SYSTEM, "Failed to launch battery optimization intent: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
                        }
                    }

                    if (viewModel.appNotificationsEnabled) {
                        permissionHandler.requestNotification(showDenial = false) {}
                    }
                }
            }

            LaunchedEffect(viewModel.isBackgroundCallsEnabled) {
                updatePipParams()
            }

            LaunchedEffect(Unit) {
                var prevRemoteVideo = false
                CallManager.isRemoteVideoOn.collect { remoteVideoOn ->
                    if (remoteVideoOn && !prevRemoteVideo) {
                        val isCalling = CallManager.callState.value == CallState.ACTIVE
                        val isBgEnabled = com.mobile.superiorchat.core.AppGraph.prefs.isBackgroundCallsEnabled
                        if (isBgEnabled && isCalling && !isInPipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!this@MainActivity.hasWindowFocus()) {
                                try {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(9, 16))
                                        .build()
                                    enterPictureInPictureMode(params)
                                } catch (e: Exception) {
                                    AppLog.log(LogCategory.SYSTEM, "Failed to enter PiP on remote video: ${e.message}", LogLevel.WARN)
                                }
                            }
                        }
                    }
                    prevRemoteVideo = remoteVideoOn
                    updatePipParams()
                }
            }

            LaunchedEffect(Unit) {
                CallManager.callState.collect { state ->
                    if (state == CallState.IDLE && isInPipMode) {
                        finishAndRemoveTask()
                    }
                    updatePipParams()
                }
            }

            LaunchedEffect(Unit) {
                CallManager.isVideoOn.collect { updatePipParams() }
            }

            SuperiorChatTheme(darkTheme = true) {
                val isUnlocked by viewModel.isAppUnlocked.collectAsState()
                val isFakeCrashBypassed = viewModel.isFakeCrashBypassed
                val isFakeCrashEnabled = viewModel.isFakeCrashEnabled
                val showTransparentDecoy = !isUnlocked && isFakeCrashEnabled && !isFakeCrashBypassed
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (showTransparentDecoy) androidx.compose.ui.graphics.Color.Transparent else Background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppScreen(
                            viewModel = viewModel,
                            requestPostNotifications = {
                                permissionHandler.requestNotification {}
                            },
                            isInPipMode = isInPipMode
                        )

                        if (showTerms) {
                            com.mobile.superiorchat.ui.components.popups.TermsAndConditionsDialog(
                                onAgree = {
                                    com.mobile.superiorchat.core.AppGraph.prefs.hasAgreedToTerms = true
                                    showTerms = false
                                },
                                onDecline = {
                                    finishAffinity()
                                }
                            )
                        } else if (showSetupUninstallDialog) {
                                    val accessInstructions = when (BuildConfig.FLAVOR) {
                                        "weather" -> "Important: The main *Chat App* is hidden inside this weather app! You can access it by searching for *Superior Chat* in the weather app search bar."
                                        "captivePortal", "playSupport" -> {
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            val qsTileNameId = context.resources.getIdentifier("qs_tile_name", "string", context.packageName)
                                            val tileName = if (qsTileNameId != 0) context.getString(qsTileNameId) else "Quick Settings"
                                            "Important: The main app has no icon! You can always access it by dialing ** *#*#9131#*#* ** or via the custom *$tileName* Quick Settings tile."
                                        }
                                        else -> "Important: You can access the app from your launcher or via secret entry points."
                                    }
                                    
                                    com.mobile.superiorchat.ui.components.popups.SetupUninstallDialog(
                                        flavor = BuildConfig.FLAVOR,
                                        accessInstructions = accessInstructions,
                                        onConfirm = {
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
    }

    private fun updateExcludeFromRecents(exclude: Boolean) {
        try {
            val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            am?.appTasks?.forEach { task ->
                task.setExcludeFromRecents(exclude)
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to update excludeFromRecents: ${e.message}", LogLevel.WARN)
        }
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isCalling = CallManager.callState.value == CallState.ACTIVE || CallManager.callState.value == CallState.CONNECTING
            val isBgEnabled = com.mobile.superiorchat.core.AppGraph.prefs.isBackgroundCallsEnabled
            val shouldAutoEnter = isBgEnabled && isCalling

            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))

            if (isCalling) {
                val endCallIntent = android.content.Intent("com.mobile.superiorchat.ACTION_END_CALL").apply {
                    setPackage(packageName)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(this, 101, endCallIntent, flags)
                val icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_call_end)
                val endCallAction = android.app.RemoteAction(icon, "End Call", "End active call", pendingIntent)
                builder.setActions(listOf(endCallAction))
            } else {
                builder.setActions(emptyList())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(shouldAutoEnter)
            }
            try {
                setPictureInPictureParams(builder.build())
            } catch (e: Exception) {
                AppLog.log(LogCategory.SYSTEM, "Failed to set PiP params: ${e.message}", LogLevel.WARN)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            wasInPipMode = true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val isCalling = CallManager.callState.value == CallState.ACTIVE || CallManager.callState.value == CallState.CONNECTING
        val isBgEnabled = com.mobile.superiorchat.core.AppGraph.prefs.isBackgroundCallsEnabled
        if (isBgEnabled && isCalling) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(9, 16))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    AppLog.log(LogCategory.SYSTEM, "Failed to enter PiP in onUserLeaveHint: ${e.message}", LogLevel.WARN)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wasInPipMode = false
        updateExcludeFromRecents(viewModel.isExcludeFromRecentsEnabled)
        updatePipParams()
        // Broadcast that chat is opened so camo engine clears notifications
        val intent = android.content.Intent("com.mobile.superiorchat.ACTION_CHAT_OPENED")
        sendBroadcast(intent)
    }

    override fun onStop() {
        super.onStop()
        // A visible floating PiP window is always in onPause (never onStop).
        // If onStop() is called while in PiP, it means the user closed/dismissed the PiP window via the '✕' button or swipe!
        if (isInPipMode || wasInPipMode || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode)) {
            wasInPipMode = false
            isInPipMode = false
            CallManager.endCall()
        } else if (isFinishing) {
            CallManager.endCall()
        }
        // Stealth requirement: audio must immediately silence if app goes to background
        com.mobile.superiorchat.media.AudioPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        try {
            unregisterReceiver(pipActionReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        CallManager.endCall()
        com.mobile.superiorchat.media.AudioPlayer.stop()
    }
}
