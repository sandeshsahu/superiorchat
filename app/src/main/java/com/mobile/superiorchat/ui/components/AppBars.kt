package com.mobile.superiorchat.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.mobile.superiorchat.R
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.NavScreen
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
//  AppBars — Top App Bar with Zen Mode + Navigation Drawer
//
//  Combines the CenterAlignedTopAppBar (with Sleeping Miku Zen Mode) and
//  the ModalDrawerSheet into one cohesive file.
//
//  Zen Mode is fully self-contained here. AppNav.kt:
//    - Passes isAnimeCharacterEnabled (to enable/disable the whole feature)
//    - Passes contentTouchTimestamp (a Long updated on every screen-content
//      touch via pointerInput) so the inactivity timer resets on any touch,
//      not just TopBar touches.
//    - Receives showMikuTip state up for placing ZenModeTipCard in the
//      Scaffold content Box at Alignment.TopCenter with zIndex(20f).
// ═══════════════════════════════════════════════════════════════════════════

/**
 * AppBars: Top App Bar with fully self-contained Zen Mode (Sleeping Miku).
 *
 * - [isAnimeCharacterEnabled] — master toggle from settings.
 * - [contentTouchTimestamp] — updated by AppNav on any screen-area touch;
 *   used to reset the 5-second inactivity countdown internally.
 * - [showMikuTip] / [onShowMikuTipChange] — hoisted up so AppNav can place
 *   ZenModeTipCard at Alignment.TopCenter inside the Scaffold content Box.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBars(
    currentScreen: NavScreen,
    isAnimeCharacterEnabled: Boolean,
    canCall: Boolean,
    contentTouchTimestamp: Long,
    showMikuTip: Boolean,
    onShowMikuTipChange: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onQrScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoMikuMode by remember { mutableStateOf(false) }
    var manualMikuMode by remember { mutableStateOf(false) }
    val isMikuActive = isAnimeCharacterEnabled && (autoMikuMode || manualMikuMode)
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Reset inactivity whenever screen content is touched
    LaunchedEffect(contentTouchTimestamp) {
        if (contentTouchTimestamp > 0L) {
            lastInteractionTime = contentTouchTimestamp
            if (autoMikuMode) autoMikuMode = false
        }
    }

    // Auto inactivity timer — polls every 500ms, activates after 5s of no touch
    LaunchedEffect(isAnimeCharacterEnabled) {
        if (!isAnimeCharacterEnabled) {
            autoMikuMode = false
            manualMikuMode = false
            return@LaunchedEffect
        }
        while (true) {
            delay(500)
            if (!manualMikuMode && !autoMikuMode) {
                if (System.currentTimeMillis() - lastInteractionTime > 5000L) {
                    autoMikuMode = true
                }
            }
        }
    }

    // Tip auto-hide after 4s
    LaunchedEffect(showMikuTip) {
        if (showMikuTip) {
            delay(4000)
            onShowMikuTipChange(false)
        }
    }

    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars
    val topPaddingPx = statusBars.getTop(density)
    val desiredHeightPx = with(density) { 52.dp.roundToPx() } + topPaddingPx

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (!isAnimeCharacterEnabled) return@clickable
                if (isMikuActive) {
                    manualMikuMode = false
                    autoMikuMode = false
                    onShowMikuTipChange(false)
                    lastInteractionTime = System.currentTimeMillis()
                } else {
                    autoMikuMode = false
                    manualMikuMode = true
                    onShowMikuTipChange(true)
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, desiredHeightPx) {
                    placeable.place(0, 0)
                }
            }
    ) {
        AnimatedContent(
            targetState = isMikuActive,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "topBarTransition",
            modifier = Modifier.fillMaxWidth()
        ) { mikuActive ->
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryLight
                    )
                },
                navigationIcon = {
                    if (currentScreen in listOf(
                            NavScreen.Permissions,
                            NavScreen.Logs,
                            NavScreen.AppSettings,
                            NavScreen.CallHistory,
                            NavScreen.AdminSettings
                        )
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryLight
                            )
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = PrimaryLight
                            )
                        }
                    }
                },
                actions = {
                    if (mikuActive) {
                        AsyncImage(
                            model = R.drawable.sleeping_miku,
                            contentDescription = "Sleeping Miku",
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                            modifier = Modifier
                                .height(56.dp)
                                .padding(end = 6.dp)
                        )
                    } else if (currentScreen == NavScreen.Chat) {
                        Row {
                            IconButton(onClick = onCallClick) {
                                Icon(
                                    imageVector = if (canCall) Icons.Filled.Phone else Icons.Filled.PhoneDisabled,
                                    contentDescription = if (canCall) "Call" else "Call Disabled",
                                    tint = PrimaryLight
                                )
                            }
                            IconButton(onClick = onQrScanClick) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = PrimaryLight
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * ZenModeTipCard: Floating tip card. Placed by AppNav in the Scaffold content
 * Box at Alignment.TopCenter with zIndex(20f), above StatusPill (zIndex 10f).
 */
@Composable
fun ZenModeTipCard(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { -it },
        exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { -it },
        modifier = modifier
            .padding(top = 16.dp)
            .padding(horizontal = 40.dp)
            .zIndex(20f)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceLevel1,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, PrimaryLight.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Zen Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tap header to exit", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  AppNavDrawer — Side Navigation Drawer Sheet
// ═══════════════════════════════════════════════════════════════════════════

/**
 * AppNavDrawer: Reusable Navigation Drawer Sheet.
 *
 * Encapsulates the side drawer menu with app branding, top-level screen
 * navigation (Chat, Profile, Application), divider, and external developer links.
 */
@Composable
fun AppNavDrawer(
    currentScreen: NavScreen,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = SurfaceLevel1,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
            .width(210.dp)
            .border(1.dp, DividerColor, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp)
        ) {
            // Header
            DrawerBrandTitle(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 24.dp)
            )

            // Navigation Items — top-level entries only
            listOf(NavScreen.Chat, NavScreen.Profile, NavScreen.AppInformation).forEach { screen ->
                val isSelected = currentScreen == screen
                val bgColor = if (isSelected) PrimaryLight else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(screen.icon, contentDescription = screen.title, tint = contentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(screen.title, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  DrawerBrandTitle — "Superior / Chat" signature lockup
//
//  Uses Montserrat ExtraBold for 'Superior' and SemiBold wide-tracked
//  'CHAT' starting precisely at the pixel column where the 'r' in
//  'Superior' ends, measured via rememberTextMeasurer.
// ═════════════════════════════════════════════════════════════════════════

@Composable
fun DrawerBrandTitle(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val superiorFontSize = 26.sp
    val chatFontSize = 13.sp

    val superiorStyle = TextStyle(
        fontFamily = MontserratFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = superiorFontSize,
        letterSpacing = (-0.5).sp
    )
    val chatStyle = TextStyle(
        fontFamily = MontserratFont,
        fontWeight = FontWeight.Bold,
        fontSize = chatFontSize,
        letterSpacing = 1.sp
    )

    // Calculate exact start padding so that 'Chat' ends exactly where 'Superior' ends ('r' end)
    val superiorMeasure = remember(superiorStyle, density) {
        textMeasurer.measure("Superior", superiorStyle)
    }
    val chatMeasure = remember(chatStyle, density) {
        textMeasurer.measure("Chat", chatStyle)
    }

    val chatStartPadding: Dp = remember(superiorMeasure, chatMeasure, density) {
        val diffPx = superiorMeasure.size.width - chatMeasure.size.width
        with(density) { diffPx.coerceAtLeast(0).toDp() }
    }

    Column(modifier = modifier) {
        Text(
            text = "Superior",
            style = superiorStyle.copy(color = PrimaryLight),
            lineHeight = superiorFontSize
        )
        Text(
            text = "CHAT",
            style = chatStyle.copy(color = PrimaryLight.copy(alpha = 0.75f)),
            lineHeight = chatFontSize,
            modifier = Modifier.padding(start = chatStartPadding, top = 2.dp)
        )
    }
}
