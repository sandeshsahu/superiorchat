package com.mobile.superiorchat.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.call.CallContainer
import com.mobile.superiorchat.ui.call.CallHistoryPage
import com.mobile.superiorchat.ui.call.CallViewModel
import com.mobile.superiorchat.ui.components.AppBars
import com.mobile.superiorchat.ui.components.AppNavDrawer
import com.mobile.superiorchat.ui.components.ZenModeTipCard
import com.mobile.superiorchat.ui.components.popups.FakeCrashDialog
import com.mobile.superiorchat.ui.components.popups.GlobalDialogHandler
import com.mobile.superiorchat.ui.components.popups.SettingsQrScanPromptDialog
import com.mobile.superiorchat.ui.components.popups.StatusPill
import com.mobile.superiorchat.ui.components.vault.VaultScreen
import com.mobile.superiorchat.ui.profile.ProfileScreen
import com.mobile.superiorchat.utils.rememberPermissionHandler
import kotlinx.coroutines.launch

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class NavScreen(val title: String, val icon: ImageVector) {
    Chat("Chat", Icons.Filled.Chat),
    Profile("Profile", Icons.Filled.Person),
    AppInformation("Application", Icons.Filled.Apps),
    Permissions("Permissions", Icons.Filled.Lock),
    Logs("App Logs", Icons.Filled.Terminal),
    AppSettings("App Settings", Icons.Filled.Settings),
    AdminSettings("Admin Settings", Icons.Filled.AdminPanelSettings),
    CallHistory("Call History", Icons.Filled.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    viewModel: MainViewModel,
    requestPostNotifications: () -> Unit,
    isInPipMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = context.findActivity()

    var currentScreen by remember { mutableStateOf(NavScreen.Chat) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val callViewModel: CallViewModel = viewModel()
    val isCallMinimized by callViewModel.isCallMinimized.collectAsState()
    val callState by CallManager.callState.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

    var showScanPrompt by remember { mutableStateOf(false) }

    // ── Zen Mode Tip Card state (Zen Mode timers/logic live in AppBars) ────
    var showMikuTip by remember { mutableStateOf(false) }
    var contentTouchTimestamp by remember { mutableLongStateOf(0L) }

    // ── System bar color controller ──────────────────────────────────────────
    LaunchedEffect(drawerState.isOpen) {
        activity?.window?.let { window ->
            val color = if (drawerState.isOpen) {
                SurfaceLevel1.toArgb()
            } else {
                Background.toArgb()
            }
            window.statusBarColor = color
            window.navigationBarColor = color
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val isTelegramApiReachable by viewModel.isTelegramApiReachable.collectAsState()
    val throttleState by com.mobile.superiorchat.bot.SendRateLimiter.throttleState.collectAsState()
    val isHeavyThrottled = throttleState is com.mobile.superiorchat.bot.ThrottleState.RateLimited || throttleState is com.mobile.superiorchat.bot.ThrottleState.GroupLimit

    // ── Global Dialog Handler (Gated on isAppUnlocked to prevent leaks over decoy) ──
    if (isAppUnlocked) {
        GlobalDialogHandler(
            dialogState = viewModel.activeGlobalDialog,
            onDismiss = { viewModel.activeGlobalDialog = null }
        )
    }

    val permissionHandler = rememberPermissionHandler { viewModel.activeGlobalDialog = it }

    // ── Lifecycle observers ──────────────────────────────────────────────────
    DisposableEffect(currentScreen, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                if (currentScreen == NavScreen.Chat) {
                    viewModel.checkTelegramConnection()
                }
            }
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (currentScreen == NavScreen.Chat) {
                viewModel.checkTelegramConnection()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.lockApp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Security: When returning from OS PiP mode back to the full app, require PIN unlock
    var wasInPipBefore by remember { mutableStateOf(false) }
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            wasInPipBefore = true
        } else if (wasInPipBefore) {
            wasInPipBefore = false
            if (AppGraph.prefs.isAppLockEnabled || AppGraph.prefs.isFakeCrashEnabled) {
                viewModel.lockApp()
            }
        }
    }

    if (currentScreen != NavScreen.Chat) {
        BackHandler {
            if (currentScreen in listOf(
                    NavScreen.Permissions,
                    NavScreen.Logs,
                    NavScreen.AppSettings,
                    NavScreen.CallHistory,
                    NavScreen.AdminSettings
                )
            ) {
                currentScreen = NavScreen.AppInformation
            } else {
                currentScreen = NavScreen.Chat
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ROOT 3-TIER WINDOW CONTAINER
    // ══════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAppUnlocked || !viewModel.isFakeCrashEnabled || viewModel.isFakeCrashBypassed) Background else Color.Transparent)
    ) {
        // ── LAYER 0: Navigation, Drawer & App Screens (Rendered only when unlocked) ──
        if (isAppUnlocked) {
            if (viewModel.isDuressModeActive) {
                VaultScreen(
                    permissionHandler = permissionHandler,
                    onGlobalDialog = { viewModel.activeGlobalDialog = it }
                )
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isInPipMode && isAppUnlocked && currentScreen in listOf(NavScreen.Chat, NavScreen.Profile, NavScreen.AppInformation),
                    scrimColor = Background.copy(alpha = 0.6f),
                    drawerContent = {
                        AppNavDrawer(
                            currentScreen = currentScreen,
                            onNavigate = { screen: NavScreen ->
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            if (!isInPipMode && (callState == CallState.IDLE || isCallMinimized)) {
                                val canCall = isNetworkAvailable && isTelegramApiReachable && viewModel.hasCredentials && !isHeavyThrottled
                                AppBars(
                                    currentScreen = currentScreen,
                                    isAnimeCharacterEnabled = viewModel.isAnimeCharacterEnabled,
                                    canCall = canCall,
                                    contentTouchTimestamp = contentTouchTimestamp,
                                    showMikuTip = showMikuTip,
                                    onShowMikuTipChange = { showMikuTip = it },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onBackClick = { currentScreen = NavScreen.AppInformation },
                                    onCallClick = { callViewModel.showCallConfirmation() },
                                    onQrScanClick = {
                                        permissionHandler.requestCamera {
                                            showScanPrompt = true
                                        }
                                    }
                                )
                            }
                        },
                        snackbarHost = {
                            SnackbarHost(snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = SurfaceLevel1,
                                    contentColor = TextPrimary,
                                    actionColor = PrimaryLight
                                )
                            }
                        },
                        containerColor = Background,
                        contentWindowInsets = WindowInsets.systemBars
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                                contentTouchTimestamp = System.currentTimeMillis()
                                            }
                                        }
                                    }
                                }
                        ) {
                            val isGlobalVideoOn by CallManager.isVideoOn.collectAsState()
                            val isGlobalRemoteVideoOn by CallManager.isRemoteVideoOn.collectAsState()
                            val isCallPipActive = isCallMinimized && (isGlobalVideoOn || isGlobalRemoteVideoOn)

                            if (!isInPipMode) {
                                StatusPill(
                                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).zIndex(10f),
                                    isCallMinimized = isCallMinimized,
                                    isCallPipActive = isCallPipActive,
                                    onRestoreCall = { callViewModel.maximizeCall() }
                                )
                            }

                            // ── Zen Mode Tip Card (Centered below header, zIndex 20f) ──
                            ZenModeTipCard(
                                visible = showMikuTip,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )

                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    val isSubScreenTarget = targetState in listOf(NavScreen.Permissions, NavScreen.Logs, NavScreen.AppSettings, NavScreen.CallHistory)
                                    val isReturningToInfo = initialState in listOf(NavScreen.Permissions, NavScreen.Logs, NavScreen.AppSettings, NavScreen.CallHistory) && targetState == NavScreen.AppInformation
                                    
                                    val isForward = isSubScreenTarget || (!isReturningToInfo && targetState.ordinal > initialState.ordinal)

                                    if (isForward) {
                                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + 
                                            fadeIn(animationSpec = tween(300))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(300)) { width -> -width / 3 } + 
                                            fadeOut(animationSpec = tween(250)))
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width / 3 } + 
                                            fadeIn(animationSpec = tween(300))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(300)) { width -> width } + 
                                            fadeOut(animationSpec = tween(250)))
                                    }
                                },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    NavScreen.Chat -> ChatScreen(
                                        onShowGlobalDialog = { viewModel.activeGlobalDialog = it },
                                        onNavigateToSettings = { 
                                            currentScreen = if (viewModel.isAdminModeEnabled) NavScreen.AdminSettings else NavScreen.AppSettings 
                                        },
                                        onNavigateToCall = { callViewModel.maximizeCall() },
                                        onNavigateToCallHistory = { currentScreen = NavScreen.CallHistory }
                                    )

                                    NavScreen.Profile -> ProfileScreen(
                                        hasCredentials = viewModel.hasCredentials,
                                        onShowGlobalDialog = { viewModel.activeGlobalDialog = it },
                                        onNavigateToSettings = { currentScreen = NavScreen.AppSettings },
                                        onClearCredentials = { 
                                            viewModel.clearCredentials()
                                        },
                                        isAutoDownloadMediaEnabled = viewModel.autoDownloadMedia,
                                        isScreenSecurityEnabled = viewModel.isScreenSecurityEnabled,
                                        isNewMessageNotificationEnabled = viewModel.newMessageNotificationEnabled,
                                        isAppNotificationsEnabled = viewModel.appNotificationsEnabled,
                                        isCallRingingEnabled = viewModel.isCallRingingEnabled,
                                        onAutoDownloadMediaChange = { viewModel.toggleAutoDownloadMedia(it) },
                                        onScreenSecurityChange = { viewModel.toggleScreenSecurity(it) },
                                        onNewMessageNotificationChange = { viewModel.toggleNewMessageNotification(it) },
                                        onAppNotificationsChange = { viewModel.toggleAppNotificationsEnabled(it) },
                                        onCallRingingChange = { viewModel.toggleCallRinging(it) },
                                        onClearChat = { deleteMedia -> viewModel.clearChat(deleteMedia) }
                                    )
                                    NavScreen.AppInformation -> {
                                        AppScreenPage(
                                            isInternetConnected = isNetworkAvailable,
                                            tokenStatus = viewModel.tokenStatusText,
                                            onNavigate = { currentScreen = it }
                                        )
                                    }
                                    NavScreen.Permissions -> {
                                        PermissionsScreen(
                                            viewModel = viewModel,
                                            permissionHandler = permissionHandler,
                                            requestPostNotifications = requestPostNotifications
                                        )
                                    }
                                    NavScreen.Logs -> {
                                        LogsScreen()
                                    }
                                    NavScreen.AppSettings -> {
                                        LaunchedEffect(Unit) {
                                            viewModel.webrtcBaseUrl = AppGraph.prefs.webrtcBaseUrl
                                        }

                                        AppSettingsPage(
                                            isInternetConnected = isNetworkAvailable,
                                            tokenStatus = viewModel.tokenStatusText,
                                            hasCredentials = viewModel.hasCredentials,
                                            botToken = viewModel.botToken,
                                            chatId = viewModel.chatId,
                                            webrtcBaseUrl = viewModel.webrtcBaseUrl,
                                            appTheme = viewModel.appTheme,
                                            onAppThemeChange = { viewModel.updateAppTheme(it) },
                                            isAnimeCharacterEnabled = viewModel.isAnimeCharacterEnabled,
                                            onAnimeCharacterChange = { viewModel.toggleAnimeCharacter(it) },
                                            isTileAccessEnabled = viewModel.tileAccessEnabled,
                                            customAccessWord = viewModel.customAccessWord,
                                            customDialerCode = viewModel.customDialerCode,
                                            isPeerLinkEnabled = viewModel.isPeerLinkEnabled,
                                            onPeerLinkChange = { viewModel.togglePeerLink(it) },
                                            isAdminModeEnabled = viewModel.isAdminModeEnabled,
                                            peerLinkPartnerBotUsername = viewModel.peerLinkPartnerBotUsername,
                                            onPeerLinkPartnerBotUsernameChange = { viewModel.updatePeerLinkPartnerUsername(it) },
                                            onBotTokenChange = { viewModel.botToken = it },
                                            onChatIdChange = { viewModel.chatId = it },
                                            onTileAccessChange = { viewModel.toggleTileAccess(it) },
                                            onCustomAccessWordChange = { viewModel.updateCustomAccessWord(it) },
                                            onCustomDialerCodeChange = { viewModel.updateCustomDialerCode(it) },
                                            onAutoDownloadMediaChange = { viewModel.toggleAutoDownloadMedia(it) },
                                            onScreenSecurityChange = { viewModel.toggleScreenSecurity(it) },
                                            onNewMessageNotificationChange = { viewModel.toggleNewMessageNotification(it) },
                                            onWebrtcBaseUrlChange = { viewModel.webrtcBaseUrl = it },
                                            isAppLockEnabled = viewModel.isAppLockEnabled,
                                            isFakeCrashEnabled = viewModel.isFakeCrashEnabled,
                                            onAppLockChange = { enabled, pin ->
                                                if (enabled) {
                                                    val prefs = AppGraph.prefs
                                                    prefs.appLockPin = com.mobile.superiorchat.utils.Security.hashSHA256(pin)
                                                    prefs.appLockPinLength = pin.length
                                                    viewModel.toggleAppLock(true)
                                                    viewModel.unlockApp(pin)
                                                } else {
                                                    val prefs = AppGraph.prefs
                                                    prefs.appLockPin = ""
                                                    viewModel.toggleAppLock(false)
                                                    viewModel.unlockApp("")
                                                }
                                            },
                                            onFakeCrashChange = { viewModel.toggleFakeCrash(it) },
                                            onChangePin = { pin ->
                                                val prefs = AppGraph.prefs
                                                prefs.appLockPin = com.mobile.superiorchat.utils.Security.hashSHA256(pin)
                                                prefs.appLockPinLength = pin.length
                                            },
                                            verifyPin = { pin -> viewModel.verifyPin(pin) },
                                            onNavigateToAdmin = { currentScreen = NavScreen.AdminSettings },
                                            onSave = { viewModel.saveCredentials() },
                                            onClearCredentials = {
                                                viewModel.clearCredentials()
                                            },
                                            onClearChat = { deleteMedia -> viewModel.clearChat(deleteMedia) },
                                            onShowGlobalDialog = { viewModel.activeGlobalDialog = it }
                                        )
                                    }
                                    NavScreen.AdminSettings -> {
                                        AdminSettingsScreen(
                                            viewModel = viewModel,
                                            onShowGlobalDialog = { viewModel.activeGlobalDialog = it }
                                        )
                                    }
                                    NavScreen.CallHistory -> {
                                        CallHistoryPage(viewModel = callViewModel)
                                    }
                                }
                            }
                            
                            if (showScanPrompt) {
                                SettingsQrScanPromptDialog(
                                    onConfirm = {
                                        showScanPrompt = false
                                        viewModel.showAppLevelQrScanner = true
                                    },
                                    onDismiss = { showScanPrompt = false }
                                )
                            }
                            
                            if (viewModel.showAppLevelQrScanner) {
                                com.mobile.superiorchat.ui.components.QrScanner(
                                    onDismiss = { viewModel.showAppLevelQrScanner = false },
                                    onSuccess = { data ->
                                        viewModel.botToken = data.token
                                        viewModel.chatId = data.chatId
                                        data.autoDownloadMedia?.let { viewModel.toggleAutoDownloadMedia(it) }
                                        data.newMessageNotification?.let { viewModel.toggleNewMessageNotification(it) }
                                        data.screenSecurity?.let { viewModel.toggleScreenSecurity(it) }
                                        data.callServer?.let { 
                                            viewModel.webrtcBaseUrl = it
                                        }
                                        data.theme?.let {
                                            try {
                                                viewModel.updateAppTheme(com.mobile.superiorchat.theme.AppTheme.valueOf(it))
                                            } catch (e: Exception) {}
                                        }
                                        
                                        viewModel.saveCredentials()
                                        viewModel.showAppLevelQrScanner = false
                                        com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "QR Configuration Applied")
                                    },
                                    onShowGlobalDialog = { viewModel.activeGlobalDialog = it }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── LAYER 1: Persistent WebRTC Calling Engine & Dialogs (Below Security Shield) ──
        CallContainer(
            callViewModel = callViewModel,
            isInPipMode = isInPipMode,
            isAppUnlocked = isAppUnlocked,
            onNavigateToSettings = { currentScreen = NavScreen.AppSettings },
            onShowGlobalDialog = { viewModel.activeGlobalDialog = it }
        )

        // ── LAYER 2: Security Shield Overlay (Topmost Layer in Full Screen) ──
        if (!isAppUnlocked && !isInPipMode) {
            if (viewModel.isFakeCrashEnabled && !viewModel.isFakeCrashBypassed) {
                // STEP 1: Fake Crash Dialog (transparent over wallpaper in TransparentActivity)
                FakeCrashDialog(
                    onBypass = { viewModel.bypassFakeCrash() }
                )
            } else if (AppGraph.prefs.isAppLockEnabled) {
                // STEP 2: PIN Lock Screen
                LockScreen(
                    pinLength = AppGraph.prefs.appLockPinLength,
                    onUnlock = { pin ->
                        val result = viewModel.unlockApp(pin)
                        result
                    }
                )
            }
        }
    }
}
