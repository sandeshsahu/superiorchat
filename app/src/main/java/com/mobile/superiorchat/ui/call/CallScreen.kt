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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.layout.ContentScale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CallScreen(
    url: String,
    isMinimized: Boolean,
    onMinimize: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by CallManager.callState.collectAsState()
    val callDuration by CallManager.callDuration.collectAsState()
    val isSpeakerphoneOn by CallManager.isSpeakerphoneOn.collectAsState()

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
            onAudioLevelChanged = { viewModel.setRemoteAudioLevel(it) }
        )
    }

    val isVideoActive = isVideoOn || isRemoteVideoOn

    // ── Shared cleanup helper (DRY) ───────────────────────────
    fun performEndCall() {
        if (!endCallSent) {
            endCallSent = true
            callEngine.triggerEndCall(webViewRef)
        }
        CallManager.endCall()
    }

    // ── BackHandler: minimize on back during active call ──────
    androidx.activity.compose.BackHandler(enabled = callState == CallState.ACTIVE && !isMinimized) {
        onMinimize()
    }

    // ── BackHandler: end call on back during connecting ───────
    androidx.activity.compose.BackHandler(enabled = callState == CallState.CONNECTING && !isMinimized) {
        performEndCall()
        onEndCall()
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

    // ── Unified Full-Screen Call Surface ────────────────────────
    Box(
        modifier = modifier
            .then(
                if (isMinimized) Modifier.size(1.dp).alpha(0f)
                else Modifier.fillMaxSize()
            )
            .background(CallBackground)
    ) {
        // 1. Fullscreen Video WebView Layer (hidden during ENDING state to reveal gradient)
        AndroidView(
            factory = { ctx ->
                WebView(ctx).also { wv ->
                    wv.setBackgroundColor(android.graphics.Color.parseColor("#0f172a"))
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
            onRelease = { wv -> wv.destroy() },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isVideoActive && callState == CallState.ACTIVE) 1f else 0f }
        )

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
                    enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
                    exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)),
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
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
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
                        onMinimize = onMinimize,
                        onEndCall = { performEndCall() }
                    )
                }
            }
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isVideoActive && isControlsVisible) Color.Black.copy(alpha = 0.45f) else Color.Transparent)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* Consume tap */ }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Encryption badge
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

            // Call status label
            CallStatusLabel(callState)

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
        // Outer pulse rings (when connecting)
        if (isConnecting) {
            repeat(2) { index ->
                PulseRing(delayMs = index * 600)
            }
        }

        // Dynamic pulse ring (when connected/active) based on volume
        if (isActive && audioLevel > 0f) {
            val dynamicScale by animateFloatAsState(
                targetValue = 1f + (audioLevel * 0.8f),
                animationSpec = tween(100, easing = LinearOutSlowInEasing)
            )
            val dynamicAlpha by animateFloatAsState(
                targetValue = 0.5f * audioLevel,
                animationSpec = tween(100, easing = LinearOutSlowInEasing)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(CallAccent.copy(alpha = dynamicAlpha.coerceIn(0f, 1f)))
            )
        }

        // Avatar circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CallAccent, Color(0xFF7C3AED))
                    )
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
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$delayMs")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_$delayMs"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_$delayMs"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(CallAccent.copy(alpha = alpha))
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
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isVideoActive) Color.Black.copy(alpha = 0.45f)
                        else CallGlass
                    )
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    onClick = onMuteToggle
                )
                ControlButton(
                    icon = if (isVideoOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    label = if (isVideoOn) "Camera On" else "Camera Off",
                    isActive = isVideoOn,
                    onClick = onVideoToggle
                )
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
            // Minimize button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CallSurface)
                        .clickable(onClick = onMinimize),
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
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(48.dp))

            // End call button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CallDanger)
                        .clickable(onClick = onEndCall),
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
                    fontSize = 11.sp
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
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White
                    else Color.White.copy(alpha = 0.1f)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = CallTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CallStatusLabel(callState: CallState) {
    when (callState) {
        CallState.CONNECTING -> {
            val infiniteTransition = rememberInfiniteTransition(label = "connecting_pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "connecting_alpha"
            )
            Text(
                text = "Calling…",
                color = CallTextPrimary.copy(alpha = alpha),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        CallState.ACTIVE -> {
            Text(
                text = "Connected",
                color = CallSuccess,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        CallState.ENDING -> {
            Text(
                text = "Call Ended",
                color = CallDanger,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
        else -> {}
    }
}

@Composable
private fun LocalCameraBox(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
        )
    }
}
