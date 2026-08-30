package com.mobile.superiorchat.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.mobile.superiorchat.core.call.CallError
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import com.mobile.superiorchat.ui.GlobalDialogState
import com.mobile.superiorchat.ui.components.popups.CallErrorDialog
import com.mobile.superiorchat.ui.components.popups.CallInitiationDialog
import com.mobile.superiorchat.ui.components.popups.IncomingCallDialog
import com.mobile.superiorchat.ui.components.popups.CallBlockedDialog
import com.mobile.superiorchat.utils.rememberPermissionHandler

/**
 * CallContainer: Layer 1 Call Surface Host
 *
 * Orchestrates the persistent WebRTC CallScreen along with all active call dialogs
 * (IncomingCallDialog, CallInitiationDialog for outbound/inbound, and CallErrorDialog).
 * Sits in Layer 1 of the root Box (below the Security Shield).
 */
@Composable
fun CallContainer(
    callViewModel: CallViewModel,
    isInPipMode: Boolean,
    isAppUnlocked: Boolean,
    onNavigateToSettings: () -> Unit,
    onShowGlobalDialog: (GlobalDialogState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val permissionHandler = rememberPermissionHandler { onShowGlobalDialog(it) }

    val callState by CallManager.callState.collectAsState()
    val isCallMinimized by callViewModel.isCallMinimized.collectAsState()
    val callConfirmationState by callViewModel.callInitiationState.collectAsState()
    val receiverConnectionState by callViewModel.receiverConnectionState.collectAsState()
    val hardwareInitTimer by callViewModel.hardwareInitTimer.collectAsState()
    val receiverHardwareTimer by callViewModel.receiverHardwareTimer.collectAsState()
    val callFailedError by CallManager.lastCallFailedDueToError.collectAsState()

    LaunchedEffect(Unit) {
        CallManager.inboundCallAcceptEvent.collectLatest {
            if (CallManager.callState.value == CallState.RINGING) {
                permissionHandler.requestAudioAndCamera {
                    callViewModel.acceptInboundCall(context)
                }
            }
        }
    }

    // ── Persistent WebRTC Call Surface ─────────────────────────────────────
    if (callState != CallState.IDLE && callState != CallState.RINGING) {
        val callUrl = CallManager.currentCallUrl
        if (callUrl != null) {
            CallScreen(
                url = callUrl,
                isMinimized = isCallMinimized || callConfirmationState != CallInitiationState.IDLE || receiverConnectionState != ReceiverConnectionState.IDLE,
                isInPipMode = isInPipMode,
                isAppUnlocked = isAppUnlocked,
                onMinimize = { callViewModel.minimizeCall() },
                onMaximize = {
                    keyboardController?.hide()
                    callViewModel.maximizeCall()
                },
                onEndCall = {
                    callViewModel.maximizeCall()
                    callViewModel.dismissReceiverState()
                },
                modifier = modifier
            )
        }
    }

    // ── Dialog Windows (Rendered ONLY when unlocked to prevent leaks over decoy) ──
    if (isAppUnlocked) {
        // ── 1. Incoming Call Dialog ──────────────────────────────────────────
        if (callState == CallState.RINGING && !isCallMinimized) {
            IncomingCallDialog(
                callerName = CallManager.incomingCallerName,
                onAccept = {
                    permissionHandler.requestAudioAndCamera {
                        callViewModel.acceptInboundCall(context)
                    }
                },
                onDecline = { callViewModel.declineInboundCall() },
                onMinimize = { callViewModel.minimizeCall() }
            )
        }

        // ── 2. Call Error Dialog ──────────────────────────────────────────────
        val shouldShowError = callFailedError != CallError.NONE &&
                !(callFailedError == CallError.DECLINED && CallManager.isIncomingCall) &&
                receiverConnectionState == ReceiverConnectionState.IDLE &&
                callConfirmationState == CallInitiationState.IDLE &&
                callState == CallState.IDLE

        if (shouldShowError) {
            CallErrorDialog(
                callError = callFailedError,
                onConfirm = {
                    val err = callViewModel.dismissCallError()
                    if (err == CallError.INVALID_URL || err == CallError.HARDWARE_ERROR) {
                        onNavigateToSettings()
                    }
                },
                onDismiss = {
                    callViewModel.dismissCallError()
                }
            )
        }

        // ── 3. Outbound Call Initiation Dialog ───────────────────────────────
        if (callConfirmationState != CallInitiationState.IDLE) {
            val isFailed = callConfirmationState == CallInitiationState.FAILED_SENDING
            val isLoading = callConfirmationState == CallInitiationState.VALIDATING ||
                    callConfirmationState == CallInitiationState.INITIALIZING_HARDWARE ||
                    callConfirmationState == CallInitiationState.SENDING_LINK

            CallInitiationDialog(
                title = when (callConfirmationState) {
                    CallInitiationState.VALIDATING -> "Validating Servers..."
                    CallInitiationState.INITIALIZING_HARDWARE -> "Initializing Hardware..."
                    CallInitiationState.SENDING_LINK -> "Sending Invite Link..."
                    CallInitiationState.FAILED_SENDING -> "Invite Link Failed"
                    CallInitiationState.SUCCESS -> "Call Started"
                    else -> "Start Secure Call"
                },
                message = when {
                    isFailed -> "Failed to deliver the invite link to Telegram. Please check your connection and try again."
                    callConfirmationState == CallInitiationState.INITIALIZING_HARDWARE -> "Accessing secure camera and microphone...\nWaiting: $hardwareInitTimer / 30 seconds"
                    else -> "A secure peer-to-peer connection link will be generated and sent to the other person's chat."
                },
                note = if (isFailed) null else "*Important:* This feature is **Experimental.** Calls may be blocked by firewalls or strict networks.\n\n**Reliability:** TURN servers are **Not Provided** by default. You must add your own to guarantee connectivity.\n\n**Security:** The developer assumes no responsibility for privacy or data leaks.\n\nRead the Security & Deployment documents on GitHub.",
                isFailed = isFailed,
                isLoading = isLoading,
                isSuccess = callConfirmationState == CallInitiationState.SUCCESS,
                onConfirm = {
                    permissionHandler.requestAudioAndCamera {
                        callViewModel.startOutboundCall(context)
                    }
                },
                onDismiss = {
                    callViewModel.cancelOutboundCall()
                }
            )
        }

        // ── 4. Receiver Connection Dialog ────────────────────────────────────
        if (receiverConnectionState != ReceiverConnectionState.IDLE) {
            val isFailed = receiverConnectionState == ReceiverConnectionState.FAILED_VALIDATING ||
                    receiverConnectionState == ReceiverConnectionState.FAILED_HARDWARE
            val isLoading = receiverConnectionState == ReceiverConnectionState.VALIDATING ||
                    receiverConnectionState == ReceiverConnectionState.INITIALIZING_HARDWARE

            CallInitiationDialog(
                title = when (receiverConnectionState) {
                    ReceiverConnectionState.VALIDATING -> "Validating Host..."
                    ReceiverConnectionState.INITIALIZING_HARDWARE -> "Initializing Hardware..."
                    ReceiverConnectionState.FAILED_VALIDATING -> "Host Unreachable"
                    ReceiverConnectionState.FAILED_HARDWARE -> "Hardware Error"
                    else -> ""
                },
                message = when (receiverConnectionState) {
                    ReceiverConnectionState.VALIDATING -> "Connecting to caller...\nWaiting: $receiverHardwareTimer / 30 seconds"
                    ReceiverConnectionState.INITIALIZING_HARDWARE -> "Accessing secure camera and microphone...\nWaiting: $receiverHardwareTimer / 30 seconds"
                    ReceiverConnectionState.FAILED_VALIDATING -> "The host is no longer calling or your network connection dropped."
                    ReceiverConnectionState.FAILED_HARDWARE -> "Could not acquire media permissions or hardware failed to start."
                    else -> ""
                },
                note = null,
                isFailed = isFailed,
                isLoading = isLoading,
                isSuccess = false,
                onConfirm = {
                    callViewModel.dismissReceiverState()
                },
                onDismiss = {
                    CallManager.endCall()
                    callViewModel.dismissReceiverState()
                }
            )
        }

        // ── 5. Call Blocked / Disabled Dialog ────────────────────────────────
        callViewModel.callBlockedDialogState?.let { data ->
            CallBlockedDialog(
                title = data.title,
                message = data.message,
                icon = data.icon,
                iconTint = data.iconTint,
                onDismiss = { callViewModel.callBlockedDialogState = null }
            )
        }
    }
}
