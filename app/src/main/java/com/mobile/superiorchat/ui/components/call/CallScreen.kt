package com.mobile.superiorchat.ui.components.call

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.core.CallManager
import com.mobile.superiorchat.core.CallState
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import com.mobile.superiorchat.theme.PrimaryLight
import kotlinx.coroutines.delay

private val Background = Color(0xFF0F172A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)
private val Success = Color(0xFF10B981)

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

    var isMuted by remember { mutableStateOf(false) }
    var isVideoOn by remember { mutableStateOf(false) }
    var isRemoteVideoOn by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Intercept back button to prevent accidental hangs
    androidx.activity.compose.BackHandler(enabled = callState == CallState.ACTIVE && !isVideoOn) {
        showEndDialog = true
    }

    if (showEndDialog) {
        ActionDialog(
            title = "Call in progress",
            message = "Do you want to minimize the call or end it completely?",
            icon = Icons.Filled.Warning,
            iconTint = PrimaryLight,
            confirmText = "Minimize",
            dismissText = "End Call",
            onConfirm = {
                showEndDialog = false
                onEndCall()
            },
            onDismiss = {
                showEndDialog = false
                webViewRef?.evaluateJavascript("window.androidEndCall();", null)
                CallManager.endCall()
                onEndCall()
            }
        )
    }

    var endCallSent by remember { mutableStateOf(false) }

    // Close screen when ending
    LaunchedEffect(callState) {
        if (callState == CallState.ENDING || callState == CallState.IDLE) {
            if (!endCallSent) {
                endCallSent = true
                webViewRef?.evaluateJavascript("window.androidEndCall();", null)
            }
            delay(1500)
            webViewRef?.clearCache(true)
            onEndCall()
        }
    }

    Box(
        modifier = modifier
            .then(
                if (isMinimized) Modifier.size(1.dp).alpha(0f) 
                else Modifier.fillMaxSize()
            )
    ) {
        // ── Hidden WebView Layer ───────────────────────────────────────
        // We load the WebRTC HTML logic here, but keep it hidden unless video is active.
        val context = LocalContext.current
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
                        domStorageEnabled = false // Disabled for stealth
                        cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE // Prevent URL forensic traces
                        mediaPlaybackRequiresUserGesture = false
                    }

                    wv.clearCache(true)

                    wv.webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            val targetUrl = request?.url?.toString() ?: ""
                            if (!targetUrl.startsWith(CallManager.VERCEL_APP_URL)) {
                                AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation to untrusted URL: $targetUrl")
                                return true
                            }
                            return false
                        }
                    }

                    wv.webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            val origin = request?.origin?.toString()?.removeSuffix("/")
                            val expectedOrigin = CallManager.VERCEL_APP_URL.removeSuffix("/")
                            
                            if (origin == expectedOrigin) {
                                AppLog.log(LogCategory.SYSTEM, "WebView granted permissions for verified origin: $origin")
                                request?.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                            } else {
                                AppLog.log(LogCategory.SYSTEM, "SECURITY: Denied WebView permissions for untrusted origin: $origin")
                                request?.deny()
                            }
                        }
                    }

                    // Javascript Bridge
                    wv.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onWebRTCEvent(action: String, data: String) {
                            when (action) {
                                "ready" -> AppLog.log(LogCategory.SYSTEM, "PeerJS Ready: $data")
                                "connected" -> CallManager.markConnected()
                                "reconnecting" -> {
                                    AppLog.log(LogCategory.SYSTEM, "WebRTC reconnecting...")
                                    // Don't end the call — ICE is trying to recover
                                }
                                "error" -> {
                                    AppLog.log(LogCategory.SYSTEM, "PeerJS Error: $data")
                                    // Only end if we never established a call
                                    if (CallManager.callState.value != CallState.ACTIVE) {
                                        CallManager.endCall()
                                    }
                                }
                                "ended" -> CallManager.endCall()
                                "remote_video" -> {
                                    isRemoteVideoOn = (data == "on")
                                }
                            }
                        }
                    }, "Android")

                    wv.loadUrl(url)
                    webViewRef = wv
                }
            },
            onRelease = { wv ->
                wv.destroy()
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isVideoOn || isRemoteVideoOn) 1f else 0f
                }
        )
    }

    if (!isMinimized) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* Handle via BackHandler internally */ },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                decorFitsSystemWindows = false
            )
        ) {
            val view = androidx.compose.ui.platform.LocalView.current
            val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            LaunchedEffect(dialogWindow) {
                dialogWindow?.setDimAmount(0f)
                dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isVideoOn || isRemoteVideoOn) Color.Transparent else Background)
            ) {
                // ── Native Overlay UI ─────────────────────────────────────────
                
                // Header (Always Visible)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isVideoOn || isRemoteVideoOn) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Encrypted Connection",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                CallStatusLabel(callState)
                if (callState == CallState.ACTIVE) {
                    Text(
                        text = formatDuration(callDuration),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Avatar (Hidden during video)
        if (!isVideoOn && !isRemoteVideoOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState == CallState.CONNECTING) {
                        ConnectingPulse()
                    }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4F46E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = callState == CallState.ACTIVE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isVideoOn) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        isActive = isMuted,
                        onClick = { 
                            isMuted = !isMuted
                            webViewRef?.evaluateJavascript("window.androidToggleMute();", null)
                        }
                    )

                    ControlButton(
                        icon = if (isVideoOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        isActive = false,
                        onClick = { 
                            isVideoOn = !isVideoOn
                            webViewRef?.evaluateJavascript("window.androidToggleVideo();", null)
                        }
                    )
                    
                    ControlButton(
                        icon = if (isSpeakerphoneOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                        isActive = isSpeakerphoneOn,
                        onClick = { 
                            CallManager.toggleSpeaker()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                        .clickable { onMinimize() }, // Minimize without unmounting
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(32.dp))

                // End Call Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                        .clickable { 
                            webViewRef?.evaluateJavascript("window.androidEndCall();", null)
                            CallManager.endCall()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } // Close Column (Controls)
        } // Close Box (UI)
        } // Close Dialog
    } // End if (!isMinimized)
} // Close CallScreen

@Composable
private fun CallStatusLabel(callState: CallState) {
    when (callState) {
        CallState.CONNECTING -> {
            val infiniteTransition = rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Text(
                text = "Calling...",
                color = TextPrimary.copy(alpha = alpha),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        CallState.ACTIVE -> {
            Text(
                text = "Connected",
                color = Success,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        CallState.ENDING -> {
            Text(
                text = "Call ended",
                color = ErrorRed,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
        else -> {}
    }
}

@Composable
private fun ConnectingPulse() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color(0xFF4F46E5).copy(alpha = alpha))
    )
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.White else Color(0xFF1E293B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
