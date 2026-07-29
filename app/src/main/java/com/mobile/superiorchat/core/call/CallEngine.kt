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
    private val onLocalVideoStateChanged: (Boolean) -> Unit
) {

    val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val targetUrl = request?.url?.toString() ?: ""
            if (!targetUrl.startsWith(CallManager.VERCEL_APP_URL)) {
                AppLog.log(LogCategory.SYSTEM, "SECURITY: Blocked navigation to untrusted URL: $targetUrl")
                return true
            }
            return false
        }
    }

    val webChromeClient = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            val origin = request?.origin?.toString()?.removeSuffix("/")
            val expected = CallManager.VERCEL_APP_URL.removeSuffix("/")
            if (origin == expected) {
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

    fun triggerEndCall(webView: WebView?) {
        webView?.evaluateJavascript("window.androidEndCall();", null)
    }
}
