package com.mobile.superiorchat.core.call

import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory

/**
 * Universal engine for handling WebRTC signaling through the WebView transport layer.
 * Groups the WebViewClient, ChromeClient, and JS Bridge logic cohesively.
 */
class CallEngine(
    private val onRemoteVideoStateChanged: (Boolean) -> Unit,
    private val onLocalVideoStateChanged: (Boolean) -> Unit,
    private val onHardwareReady: (() -> Unit)? = null,
    private val onAudioLevelChanged: ((Float) -> Unit)? = null,
    private val onVideoSwapped: ((Boolean) -> Unit)? = null
) {

    val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val targetUrl = request?.url?.toString() ?: ""
            val expectedBase = CallManager.currentBaseUrl ?: ""

            if (expectedBase.isEmpty()) return true

            try {
                val targetUri = android.net.Uri.parse(targetUrl)
                val expectedUri = android.net.Uri.parse(expectedBase)
                
                val targetHost = targetUri.host?.lowercase()
                val expectedHost = expectedUri.host?.lowercase()
                
                // 1. Strictly compare the domain host (case-insensitive)
                if (targetHost != expectedHost) {
                    AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation to untrusted origin: $targetHost")
                    return true // BLOCKED
                }
                
                // 2. Prevent port-shifting bypasses (e.g., from :3000 to :8080)
                if (targetUri.port != expectedUri.port) {
                    AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation to untrusted port: ${targetUri.port}")
                    return true // BLOCKED
                }
                
                // 3. Prevent protocol downgrade attacks (e.g., https -> http)
                if (targetUri.scheme != expectedUri.scheme) {
                    AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation due to protocol mismatch: ${targetUri.scheme}")
                    return true // BLOCKED
                }
                
            } catch (e: Exception) {
                // If the URL is malformed and cannot be parsed, block it for safety
                AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation due to malformed URL")
                return true // BLOCKED
            }
            
            return false // ALLOWED
        }
    }

    val webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            val origin = request?.origin?.toString()?.removeSuffix("/")
            val expected = CallManager.currentBaseUrl?.removeSuffix("/") ?: ""
            if (origin == expected && expected.isNotEmpty()) {
                AppLog.log(LogCategory.SYSTEM, "WebView granted permissions for verified origin: $origin")
                request?.grant(arrayOf(
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE
                ))
            } else {
                AppLog.log(LogCategory.SYSTEM, "SECURITY: Denied WebView permissions for untrusted origin: $origin")
                request?.deny()
            }
        }
    }

    @JavascriptInterface
    fun onWebRTCEvent(action: String, data: String) {
        when (action) {
            "ready" -> AppLog.log(LogCategory.SYSTEM, "PeerJS Ready: $data")
            "connected" -> CallManager.markConnected()
            "reconnecting" -> AppLog.log(LogCategory.SYSTEM, "WebRTC reconnecting...")
            "error" -> {
                AppLog.log(LogCategory.SYSTEM, "PeerJS Error: $data")
                if (CallManager.callState.value != CallState.ACTIVE) {
                    CallManager.endCall()
                }
            }
            "ended" -> CallManager.endCall()
            "remote_video" -> { onRemoteVideoStateChanged(data == "on") }
            "local_video" -> { onLocalVideoStateChanged(data == "on") }
            "hardware_ready" -> { onHardwareReady?.invoke() }
            "audio_level" -> { onAudioLevelChanged?.invoke(data.toFloatOrNull() ?: 0f) }
            "video_swapped" -> { onVideoSwapped?.invoke(data == "true") }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  JS Evaluators
    // ─────────────────────────────────────────────────────────
    fun toggleMute(webView: WebView?) {
        webView?.evaluateJavascript("window.androidToggleMute();", null)
    }

    fun toggleVideo(webView: WebView?) {
        webView?.evaluateJavascript("window.androidToggleVideo();", null)
    }

    fun toggleSwapVideo(webView: WebView?) {
        webView?.evaluateJavascript("window.androidToggleSwapVideo();", null)
    }

    fun flipCamera(webView: WebView?) {
        webView?.evaluateJavascript("window.androidFlipCamera();", null)
    }

    fun triggerEndCall(webView: WebView?) {
        webView?.evaluateJavascript("window.androidEndCall();", null)
    }

    fun setPipMode(webView: WebView?, isEnabled: Boolean, targetVideo: String) {
        webView?.evaluateJavascript("window.androidSetPipMode($isEnabled, '$targetVideo');", null)
    }
}
