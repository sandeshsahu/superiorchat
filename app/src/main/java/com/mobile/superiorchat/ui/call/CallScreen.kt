package com.mobile.superiorchat.ui.call

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobile.superiorchat.ui.skeletonEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import com.mobile.superiorchat.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.superiorchat.core.call.CallEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.mobile.superiorchat.ui.components.bounceClick

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CallScreen(
    url: String,
    isMinimized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by CallManager.callState.collectAsState()
    val callDuration by CallManager.callDuration.collectAsState()
    val isSpeakerphoneOn by CallManager.isSpeakerphoneOn.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val viewModel: CallViewModel = viewModel()
    val isMuted by viewModel.isMuted.collectAsState()
    val isVideoOn by viewModel.isVideoOn.collectAsState()
    val isRemoteVideoOn by viewModel.isRemoteVideoOn.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isSwappedVideo by viewModel.isSwappedVideo.collectAsState()
    val remoteAudioLevel by viewModel.remoteAudioLevel.collectAsState()
    val profilePhotoPath by viewModel.profilePhotoPath.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    var endCallSent by remember { mutableStateOf(false) }

    val callEngine = remember {
        CallEngine(
            onRemoteVideoStateChanged = { viewModel.setRemoteVideo(it) },
            onLocalVideoStateChanged = { viewModel.setLocalVideo(it) },
            onHardwareReady = { viewModel.onHardwareReady() },
            onAudioLevelChanged = { viewModel.setRemoteAudioLevel(it) },
            onVideoSwapped = { viewModel.setSwappedVideo(it) }
        )
    }

    val isVideoActive = isVideoOn || isRemoteVideoOn

    // ── Fix #1: Animated WebView alpha — prevents hard flash on video appear/disappear ──
    val webViewAlpha by animateFloatAsState(
        targetValue = if (isVideoActive && callState == CallState.ACTIVE) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "webViewAlpha"
    )

    // ENDING state: very subtle darkening — NOT blood red.
    // Premium apps keep dark/neutral bg; color comes from text, not the whole screen.
    val screenBgColor by animateColorAsState(
        targetValue = if (callState == CallState.ENDING) Color(0xFF080C14) else CallBackground,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "screenBg"
    )


    // ── Shared cleanup helper (DRY) ───────────────────────────
    fun performEndCall() {
        if (!endCallSent) {
            endCallSent = true
            callEngine.triggerEndCall(webViewRef)
        }
        CallManager.endCall()
    }

    val coroutineScope = rememberCoroutineScope()
    var isMaximizing by remember { mutableStateOf(false) }
    var isMinimizing by remember { mutableStateOf(false) }

    val handleMinimize = {
        coroutineScope.launch {
            isMinimizing = true
            delay(50)
            onMinimize()
            delay(150)
            isMinimizing = false
        }
    }

    // ── BackHandler: minimize on back during active or connecting call ──────
    androidx.activity.compose.BackHandler(
        enabled = (callState == CallState.ACTIVE || callState == CallState.CONNECTING) && !isMinimized
    ) {
        handleMinimize()
    }

    // ── Auto-close when call transitions to ENDING/IDLE ──────
    LaunchedEffect(callState) {
        if (callState == CallState.ENDING || callState == CallState.IDLE) {
            performEndCall()
            delay(600)
            webViewRef?.clearCache(true)
            onEndCall()
        }
    }

    // ── Background Call Terminator (Stealth Mode) ──────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                // End the call immediately when the app goes to the background
                if (callState == CallState.ACTIVE || callState == CallState.CONNECTING) {
                    performEndCall()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Picture in Picture Mode ──────────────────────────────
    val showPip = isMinimized && (isVideoOn || isRemoteVideoOn) && callState == CallState.ACTIVE

    LaunchedEffect(showPip, isVideoOn, isRemoteVideoOn, callState) {
        val targetVideo = if (isRemoteVideoOn) "remote" else if (isVideoOn) "local" else "none"
        if (showPip) {
            callEngine.setPipMode(webViewRef, true, targetVideo)
        } else {
            callEngine.setPipMode(webViewRef, false, "none")
        }
    }

    // ── PiP Animations & Gestures ────────────────────────────
    val pipOffsetX = remember { Animatable(0f) }
    val pipOffsetY = remember { Animatable(0f) }

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isMinimized && showPip) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pipRadius"
    )

    // ── Unified Full-Screen Call Surface ────────────────────────
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val pipWidthPx = with(density) { 110.dp.toPx() }
        val pipHeightPx = with(density) { 160.dp.toPx() }
        val paddingEndPx = with(density) { 16.dp.toPx() }
        val paddingBottomPx = with(density) { 100.dp.toPx() }

        // Since we align to BottomEnd, max negative offsets represent the top and left edges
        val minOffsetX = -(maxWidthPx - pipWidthPx - paddingEndPx * 2)
        val minOffsetY = -(maxHeightPx - pipHeightPx - paddingBottomPx * 2)

        // We use a secondary Box to manage the PiP dragging and size
        Box(
            modifier = Modifier
                .then(
                    if (isMinimized) {
                        if (showPip) {
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset { IntOffset(pipOffsetX.value.roundToInt(), pipOffsetY.value.roundToInt()) }
                                .padding(end = 16.dp, bottom = 100.dp)
                                .imePadding() // Keyboard only pushes up the PiP window, not the fullscreen video
                                .size(110.dp, 160.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            coroutineScope.launch {
                                                // Snap to nearest horizontal edge (left or right)
                                                val targetX = if (pipOffsetX.value < minOffsetX / 2) minOffsetX else 0f
                                                // Keep Y exactly where it was dragged, but coerce within safe bounds
                                                val targetY = pipOffsetY.value.coerceIn(minOffsetY, 0f)

                                                // Fix #5: Snappier PiP edge-snap — medium bouncy on X for natural feel, no bounce on Y
                                                launch { pipOffsetX.animateTo(targetX, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
                                                launch { pipOffsetY.animateTo(targetY, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            coroutineScope.launch {
                                                pipOffsetX.snapTo(pipOffsetX.value + dragAmount.x)
                                                pipOffsetY.snapTo(pipOffsetY.value + dragAmount.y)
                                            }
                                        }
                                    )
                                }
                        } else {
                            Modifier.align(Alignment.BottomEnd).size(1.dp).alpha(0f)
                        }
                    } else Modifier.fillMaxSize()
                )
                // Fix #6: Slightly longer animateContentSize so it doesn't race with skeleton fadeout
                .animateContentSize(animationSpec = tween(280, easing = FastOutSlowInEasing))
                .clip(RoundedCornerShape(animatedCornerRadius))
                .background(screenBgColor)
        ) {
            // 1. Fullscreen Video WebView Layer (hidden during ENDING state to reveal gradient)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).also { wv ->
                        wv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        wv.layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        wv.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = false
                            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            mediaPlaybackRequiresUserGesture = false
                        }
                        wv.clearCache(true)

                        wv.webViewClient = callEngine.webViewClient
                        wv.webChromeClient = callEngine.webChromeClient

                        wv.addJavascriptInterface(callEngine, "Android")

                        wv.loadUrl(url)
                        webViewRef = wv
                    }
                },
                update = { wv ->
                    wv.visibility = if (isMaximizing || isMinimizing) android.view.View.GONE else android.view.View.VISIBLE
                    if (isMinimized && showPip) {
                        wv.outlineProvider = object : android.view.ViewOutlineProvider() {
                            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                val radius = 16f * view.context.resources.displayMetrics.density
                                outline.setRoundRect(0, 0, view.width, view.height, radius)
                            }
                        }
                        wv.clipToOutline = true
                    } else {
                        wv.outlineProvider = null
                        wv.clipToOutline = false
                    }
                },
                onRelease = { wv -> wv.destroy() },
                modifier = Modifier
                    .fillMaxSize()
                    // Fix #1: Smooth fade instead of instant alpha flip
                    .graphicsLayer { alpha = webViewAlpha }
            )

            // 1.1. Background Video Loading Skeleton
            AnimatedVisibility(
                visible = (callState == CallState.CONNECTING && (isVideoOn || isRemoteVideoOn)) || isMaximizing || isMinimizing,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.fillMaxSize()
            ) {
                com.mobile.superiorchat.ui.SkeletonGalleryItem(modifier = Modifier.fillMaxSize())
            }

            // 1.5. PiP Touch Interceptor Layer
            if (isMinimized && showPip) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                isMaximizing = true
                                // 1. Hide WebView FIRST while still small (prevents SurfaceFlinger glitch)
                                delay(50)
                                // 2. Trigger Compose layout expansion
                                onMaximize()
                                // 3. Wait for SurfaceFlinger to reallocate 1080x2400 buffer
                                delay(150)
                                // 4. Reveal perfectly sized WebView
                                isMaximizing = false
                            }
                        }
                )
            }

            // 2. Native Compose UI Overlay Layer (only active when not minimized)
            if (!isMinimized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.toggleControls()
                        }
                        .systemBarsPadding()
                ) {
                    // ── Top Header ────────────────────────────────
                    CallHeader(
                        callState = callState,
                        callDuration = callDuration,
                        isVideoActive = isVideoActive,
                        isControlsVisible = isControlsVisible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )

                    // ── Center Avatar (visible during audio, connecting, or call ended) ──────
                    AnimatedVisibility(
                        visible = (!isRemoteVideoOn && !isSwappedVideo) || callState == CallState.ENDING,
                        // Fix #8: scaleIn now uses spring for organic feel instead of linear tween
                        enter = fadeIn(tween(350, easing = FastOutSlowInEasing)) + scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        ),
                        exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) + scaleOut(
                            targetScale = 0.88f,
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        CallAvatar(
                            isConnecting = callState == CallState.CONNECTING,
                            isActive = callState == CallState.ACTIVE,
                            profilePhotoPath = profilePhotoPath,
                            audioLevel = remoteAudioLevel,
                            modifier = Modifier.padding(bottom = 80.dp)
                        )
                    }

                    // ── Floating Local/Remote Camera Preview Box Container ────────────
                    // NOTE: padding and size are FROZEN — must stay in sync with call.css body.is-host rules
                    LocalCameraBox(
                        isVisible = (if (isSwappedVideo) isRemoteVideoOn else isVideoOn) && callState == CallState.ACTIVE,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 160.dp, end = 16.dp)
                            .clickable {
                                viewModel.toggleSwapVideo()
                                callEngine.toggleSwapVideo(webViewRef)
                            }
                    )

                    // ── Bottom Controls ──────────────────────────
                    AnimatedVisibility(
                        visible = isControlsVisible && callState != CallState.ENDING,
                        // Fix #4: Slide only 1/3 height with spring — much less jarring than full-height slide
                        enter = fadeIn(tween(350, easing = FastOutSlowInEasing)) + slideInVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                            initialOffsetY = { it / 3 }
                        ),
                        exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) + slideOutVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                            targetOffsetY = { it / 3 }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedVisibility(visible = callState == CallState.CONNECTING) {
                                ConnectingInfoBanner()
                            }

                            CallControls(
                                callState = callState,
                                isMuted = isMuted,
                                isVideoOn = isVideoOn,
                                isSpeakerphoneOn = isSpeakerphoneOn,
                                isVideoActive = isVideoActive,
                                onMuteToggle = {
                                    viewModel.toggleMute()
                                    callEngine.toggleMute(webViewRef)
                                },
                                onVideoToggle = {
                                    viewModel.toggleVideo()
                                    callEngine.toggleVideo(webViewRef)
                                },
                                onSpeakerToggle = { CallManager.toggleSpeaker() },
                                onFlipCamera = { callEngine.flipCamera(webViewRef) },
                                onMinimize = { handleMinimize() },
                                onEndCall = { performEndCall() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Connecting Info Banner — extracted composable
// ─────────────────────────────────────────────────────────────

@Composable
private fun ConnectingInfoBanner() {
    Box(
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CallSurface.copy(alpha = 0.70f))
            .border(1.dp, CallAccent.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
    ) {
        // Left accent gradient bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(CallAccent, Color(0xFF7C3AED))))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 19.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "The Invitation link has been sent to Telegram.\nThe call will end if not answered within 45s.",
                color = PrimaryLight,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Header — Encryption badge, status, duration
// ─────────────────────────────────────────────────────────────

@Composable
private fun CallHeader(
    callState: CallState,
    callDuration: Long,
    isVideoActive: Boolean,
    isControlsVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Fix #2: Animated header backdrop — prevents instant color pop when controls toggle
    val headerBgColor by animateColorAsState(
        targetValue = if (isVideoActive && isControlsVisible) Color.Black.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "headerBg"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(headerBgColor)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* Consume tap */ }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Encryption badge — hidden during ENDING (call is done, not relevant)
            AnimatedVisibility(
                visible = callState != CallState.ENDING,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CallSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = CallSuccess,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "END-TO-END ENCRYPTED",
                            color = CallSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Call status label — AnimatedContent for smooth state-to-state transitions
            CallStatusLabel(callState, callDuration)

            // Duration timer
            AnimatedVisibility(
                visible = callState == CallState.ACTIVE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = CallManager.formatDuration(callDuration),
                    color = CallTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Avatar — Centered circle with connecting pulse
// ─────────────────────────────────────────────────────────────

@Composable
private fun CallAvatar(
    isConnecting: Boolean,
    isActive: Boolean = false,
    profilePhotoPath: String? = null,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Fix #10: 3 pulse rings, organically staggered (0 / 500 / 1000ms)
        if (isConnecting) {
            repeat(3) { index ->
                PulseRing(delayMs = index * 500)
            }
        }

        // Fix #11: Audio-reactive ring — spring spec for instant response, color shifts accent→green
        if (isActive && audioLevel > 0f) {
            val dynamicScale by animateFloatAsState(
                targetValue = 1f + (audioLevel * 0.8f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
                label = "audioScale"
            )
            val dynamicAlpha by animateFloatAsState(
                targetValue = 0.5f * audioLevel,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
                label = "audioAlpha"
            )
            // Color lerps from CallAccent → CallSuccess as audio level rises (just speaking → clearly talking)
            val ringColor = lerp(CallAccent, CallSuccess, audioLevel.coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = dynamicScale
                        scaleY = dynamicScale
                        this.alpha = dynamicAlpha.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(ringColor)
            )
        }

        // Avatar circle — clean, no ENDING-specific tint or border
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .then(
                    if (isConnecting) Modifier.skeletonEffect()
                    else Modifier.background(Brush.linearGradient(colors = listOf(CallAccent, Color(0xFF7C3AED))))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoPath != null && File(profilePhotoPath).exists()) {
                AsyncImage(
                    model = File(profilePhotoPath),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun PulseRing(delayMs: Int) {
    // Fix #10: Sine-in-out bezier — organic, natural pulse (vs mechanical FastOutSlowIn)
    val sineInOut = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$delayMs")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMs, easing = sineInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_$delayMs"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delayMs, easing = sineInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_$delayMs"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(CallAccent)
    )
}

// ─────────────────────────────────────────────────────────────
//  Controls — Mute, Video, Speaker, Minimize, End
// ─────────────────────────────────────────────────────────────

@Composable
private fun CallControls(
    callState: CallState,
    isMuted: Boolean,
    isVideoOn: Boolean,
    isSpeakerphoneOn: Boolean,
    isVideoActive: Boolean,
    onMuteToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    onMinimize: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null
        ) { /* Consume tap */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Toggle controls (visible when call is active) ────
        AnimatedVisibility(
            visible = callState == CallState.ACTIVE,
            // Fix #9: Toggle row slide also uses 1/3 offset + spring
            enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) + slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                initialOffsetY = { it / 3 }
            ),
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + slideOutVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                targetOffsetY = { it / 3 }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    // Fix #16: Proper frosted glass with gradient + border — much more depth than flat CallGlass
                    .background(
                        brush = if (isVideoActive)
                            Brush.linearGradient(listOf(Color.Black.copy(0.58f), Color.Black.copy(0.38f)))
                        else
                            Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.05f)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    activeIconTint = Color.Black,
                    activeBg = CallDanger.copy(alpha = 0.90f),
                    onClick = onMuteToggle
                )
                ControlButton(
                    icon = if (isVideoOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    label = if (isVideoOn) "Camera On" else "Camera Off",
                    isActive = isVideoOn,
                    onClick = onVideoToggle
                )
                if (isVideoOn) {
                    ControlButton(
                        icon = Icons.Filled.FlipCameraAndroid,
                        label = "Flip",
                        isActive = false,
                        onClick = onFlipCamera
                    )
                }
                ControlButton(
                    icon = if (isSpeakerphoneOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                    label = if (isSpeakerphoneOn) "Loudspeaker" else "Earpiece",
                    isActive = isSpeakerphoneOn,
                    onClick = onSpeakerToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Bottom action row: Minimize + End ────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fix #17: Minimize button with proper spring interaction + glassmorphism style
            val minimizeInteraction = remember { MutableInteractionSource() }
            val minimizePressed by minimizeInteraction.collectIsPressedAsState()
            val minimizeScale by animateFloatAsState(
                targetValue = if (minimizePressed) 0.87f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                label = "minimizeScale"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = minimizeScale; scaleY = minimizeScale }
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color.White.copy(0.13f), Color.White.copy(0.07f)))
                        )
                        .border(1.dp, Color.White.copy(0.10f), CircleShape)
                        .clickable(
                            interactionSource = minimizeInteraction,
                            indication = null,
                            onClick = onMinimize
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Minimize",
                    color = CallTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            // Fix #18: End call button with proper spring interaction
            val endCallInteraction = remember { MutableInteractionSource() }
            val endCallPressed by endCallInteraction.collectIsPressedAsState()
            val endCallScale by animateFloatAsState(
                targetValue = if (endCallPressed) 0.87f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                label = "endCallScale"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { scaleX = endCallScale; scaleY = endCallScale }
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(CallDanger.copy(0.85f), CallDanger))
                        )
                        .clickable(
                            interactionSource = endCallInteraction,
                            indication = null,
                            onClick = onEndCall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "End",
                    color = CallDanger.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Reusable Components
// ─────────────────────────────────────────────────────────────

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    // Optional overrides for specific active-state styling (e.g. mute = red bg)
    activeBg: Color = Color.White,
    activeIconTint: Color = Color.Black,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Fix #7: Snappier, cleaner bounce — LowBouncy + StiffnessMedium stops wobbling too long
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.87f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonScale"
    )

    // Smooth background color transition on active toggle
    val bgColor by animateColorAsState(
        targetValue = if (isActive) activeBg else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "buttonBg"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Fix #18: Crossfade for lag-free icon swap (mute ↔ unmute, video on ↔ off)
            Crossfade(
                targetState = icon,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "iconCrossfade"
            ) { currentIcon ->
                Icon(
                    imageVector = currentIcon,
                    contentDescription = label,
                    tint = if (isActive) activeIconTint else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = CallTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CallStatusLabel(callState: CallState, callDuration: Long = 0L) {
    // Fix #2: AnimatedContent — smooth crossfade between state text instead of instant swap
    AnimatedContent(
        targetState = callState,
        transitionSpec = {
            (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.93f, animationSpec = tween(260, easing = FastOutSlowInEasing))) togetherWith
            (fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 0.93f, animationSpec = tween(180, easing = FastOutSlowInEasing)))
        },
        label = "callStatus"
    ) { state ->
        when (state) {
            CallState.CONNECTING -> {
                val dots = rememberAnimatedDots()
                val infiniteTransition = rememberInfiniteTransition(label = "connecting_pulse")
                val textAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.50f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "connecting_alpha"
                )
                Text(
                    text = "Waiting$dots",
                    color = CallTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer { this.alpha = textAlpha }
                )
            }
            CallState.ACTIVE -> {
                // Live indicator dot + "Connected" text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    LiveDot()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected",
                        color = CallSuccess,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            CallState.ENDING -> {
                // Clean, minimal ended state — dark bg + white text, NOT red-everything.
                // Red accent only on the small icon, which is enough signal.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = null,
                        tint = CallDanger.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Call Ended",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (callDuration > 0L) {
                        Text(
                            text = CallManager.formatDuration(callDuration),
                            color = CallTextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            else -> {
                // Stable-height placeholder during IDLE (avoids layout shift in AnimatedContent)
                Text(text = "", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LocalCameraBox(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Fix #20: Animated border shimmer — creates subtle life without touching position or size
    val infiniteTransition = rememberInfiniteTransition(label = "camBorderShimmer")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camBorderAlpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = tween(200)),
        modifier = modifier
    ) {
        // ── FROZEN: size and shape must stay in sync with call.css body.is-host #localVideo ──
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
        )
    }
}

// ─────────────────────────────────────────────────────────────
//  Utility Composables
// ─────────────────────────────────────────────────────────────

/** Returns an animated ellipsis string cycling "." → ".." → "..." every 500ms. */
@Composable
private fun rememberAnimatedDots(): String {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L)
            dotCount = if (dotCount >= 3) 1 else dotCount + 1
        }
    }
    return ".".repeat(dotCount)
}

/** A small pulsing green live-indicator dot shown next to "Connected". */
@Composable
private fun LiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "liveDot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotScale"
    )
    Box(
        modifier = Modifier
            .size(9.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(CallSuccess)
    )
}
