package com.mobile.superiorutils.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mobile.superiorutils.theme.*
import kotlinx.coroutines.launch

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class NavScreen(val title: String, val icon: ImageVector) {
    Chat("Chat", Icons.Filled.Dashboard),
    Permissions("Permissions", Icons.Filled.Lock),
    Logs("Logs", Icons.AutoMirrored.Filled.List),
    Settings("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    viewModel: MainViewModel,
    requestPostNotifications: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(NavScreen.Chat) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activity = context as? android.app.Activity
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

    val permissionStatus by viewModel.permissionStatus.collectAsState()

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions()
    }

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

    val permissionStates = listOf(
        PermissionState("Post Notifications", permissionStatus.hasPostNotifs) { requestPostNotifications() },
        PermissionState("Camera", permissionStatus.hasCamera) {
            singlePermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        PermissionState("Microphone", permissionStatus.hasMicrophone) {
            singlePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        PermissionState(
            name = "Media & Storage",
            isGranted = permissionStatus.mediaAccessLevel == MediaAccessLevel.FULL,
            displayStatus = when (permissionStatus.mediaAccessLevel) {
                MediaAccessLevel.FULL -> "Granted"
                MediaAccessLevel.PARTIAL -> "Partial Access"
                MediaAccessLevel.NONE -> "Required"
            }
        ) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    multiPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                    )
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> {
                    multiPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    )
                }
                else -> {
                    singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        },
        PermissionState("Ignore Battery Optimizations", permissionStatus.hasIgnoreBattery) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
        }
    )

    if (currentScreen != NavScreen.Chat) {
        BackHandler {
            currentScreen = NavScreen.Chat
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Background.copy(alpha = 0.6f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceLevel1,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .width(240.dp)
                    .border(1.dp, DividerColor, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text("Superior Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Author Sandesh", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Navigation Items (excluding Settings — accessed via gear icon)
                    NavScreen.entries.filter { it != NavScreen.Settings }.forEach { screen ->
                        val isSelected = currentScreen == screen
                        val bgColor = if (isSelected) Primary.copy(alpha = 0.3f) else Color.Transparent
                        val contentColor = if (isSelected) TextPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable {
                                    currentScreen = screen
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(screen.icon, contentDescription = screen.title, tint = contentColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(screen.title, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 24.dp))

                    // External Links
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ExternalLinkItem("LinkedIn", Icons.Filled.Link)
                        ExternalLinkItem("GitHub", Icons.Filled.Code)
                        ExternalLinkItem("GitLab", Icons.Filled.Terminal)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(currentScreen.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    },
                    navigationIcon = {
                        if (currentScreen == NavScreen.Settings) {
                            IconButton(onClick = { currentScreen = NavScreen.Chat }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    actions = {
                        if (currentScreen == NavScreen.Chat || currentScreen == NavScreen.Logs) {
                            IconButton(onClick = { currentScreen = NavScreen.Settings }) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Background,
                        scrolledContainerColor = Background
                    )
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SurfaceLevel1,
                        contentColor = TextPrimary,
                        actionColor = Primary
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
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) { width -> width / 4 } + 
                            fadeIn(animationSpec = tween(300)) + 
                            scaleIn(initialScale = 0.95f, animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) { width -> -width / 4 } + 
                            fadeOut(animationSpec = tween(250)) + 
                            scaleOut(targetScale = 0.95f, animationSpec = tween(250)))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        NavScreen.Chat -> ChatScreen()
                        NavScreen.Permissions -> PermissionsScreen(permissions = permissionStates)
                        NavScreen.Logs -> LogsScreen()
                        NavScreen.Settings -> SettingsScreen(
                            isInternetConnected = permissionStatus.isInternetConnected,
                            botToken = viewModel.botToken,
                            chatId = viewModel.chatId,
                            isAutoDownloadMediaEnabled = viewModel.autoDownloadMedia,
                            onBotTokenChange = { viewModel.botToken = it },
                            onChatIdChange = { viewModel.chatId = it },
                            onAutoDownloadMediaChange = { viewModel.toggleAutoDownloadMedia(it) },
                            onSave = {
                                viewModel.saveCredentials()
                                Toast.makeText(context, "Credentials Saved", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalLinkItem(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
