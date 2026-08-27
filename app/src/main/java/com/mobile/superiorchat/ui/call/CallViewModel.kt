package com.mobile.superiorchat.ui.call

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import com.mobile.superiorchat.core.call.CallError
import com.mobile.superiorchat.bot.InlineKeyboardButton
import com.mobile.superiorchat.bot.InlineKeyboardMarkup
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.mobile.superiorchat.data.entity.CallHistoryNode

enum class CallInitiationResult { SUCCESS, VALIDATION_FAILED, TELEGRAM_FAILED, HARDWARE_INIT }

enum class CallInitiationState { IDLE, CONFIRMATION, VALIDATING, INITIALIZING_HARDWARE, SENDING_LINK, FAILED_SENDING, SUCCESS }
enum class ReceiverConnectionState { IDLE, VALIDATING, INITIALIZING_HARDWARE, FAILED_VALIDATING, FAILED_HARDWARE }

class CallViewModel : ViewModel() {

    private var lastCallEventMsgId: Long = 0L
    private var lastCallEventTime: Long = 0L

    private val _isCallMinimized = MutableStateFlow(false)
    val isCallMinimized: StateFlow<Boolean> = _isCallMinimized.asStateFlow()

    private val _callInitiationState = MutableStateFlow(CallInitiationState.IDLE)
    val callInitiationState: StateFlow<CallInitiationState> = _callInitiationState.asStateFlow()

    private val _receiverConnectionState = MutableStateFlow(ReceiverConnectionState.IDLE)
    val receiverConnectionState: StateFlow<ReceiverConnectionState> = _receiverConnectionState.asStateFlow()

    private val _hardwareInitTimer = MutableStateFlow(0)
    val hardwareInitTimer: StateFlow<Int> = _hardwareInitTimer.asStateFlow()

    private val _receiverHardwareTimer = MutableStateFlow(0)
    val receiverHardwareTimer: StateFlow<Int> = _receiverHardwareTimer.asStateFlow()

    private var outboundTimerJob: kotlinx.coroutines.Job? = null
    private var receiverTimerJob: kotlinx.coroutines.Job? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isIncomingAudioMuted = MutableStateFlow(false)
    val isIncomingAudioMuted: StateFlow<Boolean> = _isIncomingAudioMuted.asStateFlow()

    private val _isVideoOn = MutableStateFlow(false)
    val isVideoOn: StateFlow<Boolean> = _isVideoOn.asStateFlow()

    private val _isRemoteVideoOn = MutableStateFlow(false)
    val isRemoteVideoOn: StateFlow<Boolean> = _isRemoteVideoOn.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    private val _validationPassedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val validationPassedEvent = _validationPassedEvent.asSharedFlow()

    private val _hardwareReadyEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val hardwareReadyEvent = _hardwareReadyEvent.asSharedFlow()

    private val _errorEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private var currentTelegramUrl: String? = null

    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isSwappedVideo = MutableStateFlow(false)
    val isSwappedVideo: StateFlow<Boolean> = _isSwappedVideo.asStateFlow()

    private val _remoteAudioLevel = MutableStateFlow(0f)
    val remoteAudioLevel: StateFlow<Float> = _remoteAudioLevel.asStateFlow()

    private val _profilePhotoPath = MutableStateFlow<String?>(null)
    val profilePhotoPath: StateFlow<String?> = _profilePhotoPath.asStateFlow()

    init {
        // Handle Validation Passed Callback from WebRTC
        viewModelScope.launch {
            validationPassedEvent.collectLatest {
                if (_receiverConnectionState.value == ReceiverConnectionState.VALIDATING) {
                    _receiverConnectionState.value = ReceiverConnectionState.INITIALIZING_HARDWARE
                    _receiverHardwareTimer.value = 0
                }
            }
        }

        // Handle Hardware Ready Callback from WebRTC
        viewModelScope.launch {
            hardwareReadyEvent.collectLatest {
                if (_receiverConnectionState.value == ReceiverConnectionState.VALIDATING || _receiverConnectionState.value == ReceiverConnectionState.INITIALIZING_HARDWARE) {
                    receiverTimerJob?.cancel()
                    _receiverConnectionState.value = ReceiverConnectionState.IDLE
                    _isCallMinimized.value = false
                } else if (_callInitiationState.value == CallInitiationState.INITIALIZING_HARDWARE) {
                    outboundTimerJob?.cancel()
                    _callInitiationState.value = CallInitiationState.SENDING_LINK
                    val result = sendTelegramLink()
                    
                    if (_callInitiationState.value == CallInitiationState.SENDING_LINK) {
                        if (result == CallInitiationResult.SUCCESS) {
                            _callInitiationState.value = CallInitiationState.SUCCESS
                            kotlinx.coroutines.delay(500)
                            _callInitiationState.value = CallInitiationState.IDLE
                            _isCallMinimized.value = false
                        } else {
                            _callInitiationState.value = CallInitiationState.FAILED_SENDING
                        }
                    }
                }
            }
        }

        // Handle WebRTC/CallEngine Error Callbacks
        viewModelScope.launch {
            errorEvent.collectLatest { errorMsg ->
                if (_receiverConnectionState.value == ReceiverConnectionState.VALIDATING || _receiverConnectionState.value == ReceiverConnectionState.INITIALIZING_HARDWARE) {
                    receiverTimerJob?.cancel()
                    if (errorMsg.contains("Host unreachable", ignoreCase = true) || errorMsg.contains("expired", ignoreCase = true) || errorMsg.contains("unreachable", ignoreCase = true)) {
                        _receiverConnectionState.value = ReceiverConnectionState.FAILED_VALIDATING
                    } else {
                        _receiverConnectionState.value = ReceiverConnectionState.FAILED_HARDWARE
                    }
                }
            }
        }

        // Reset minimize state if call ends
        viewModelScope.launch {
            CallManager.callState.collectLatest { state ->
                if (state == CallState.IDLE) {
                    _isCallMinimized.value = false
                }
            }
        }
    }

    fun showCallConfirmation() {
        _callInitiationState.value = CallInitiationState.CONFIRMATION
    }

    fun minimizeCall() {
        _isCallMinimized.value = true
    }

    fun maximizeCall() {
        _isCallMinimized.value = false
    }

    fun cancelOutboundCall() {
        outboundTimerJob?.cancel()
        if (_callInitiationState.value != CallInitiationState.CONFIRMATION) {
            CallManager.endCall()
        }
        _callInitiationState.value = CallInitiationState.IDLE
    }

    fun startOutboundCall(context: Context) {
        if (_callInitiationState.value == CallInitiationState.CONFIRMATION || _callInitiationState.value == CallInitiationState.FAILED_SENDING) {
            _callInitiationState.value = CallInitiationState.VALIDATING
            viewModelScope.launch {
                val result = initiateCall(context)
                if (_callInitiationState.value == CallInitiationState.VALIDATING) {
                    when (result) {
                        CallInitiationResult.HARDWARE_INIT -> {
                            _callInitiationState.value = CallInitiationState.INITIALIZING_HARDWARE
                            _hardwareInitTimer.value = 0
                            outboundTimerJob?.cancel()
                            outboundTimerJob = viewModelScope.launch {
                                while (_hardwareInitTimer.value < 30 && _callInitiationState.value == CallInitiationState.INITIALIZING_HARDWARE) {
                                    kotlinx.coroutines.delay(1000)
                                    _hardwareInitTimer.value++
                                }
                                if (_callInitiationState.value == CallInitiationState.INITIALIZING_HARDWARE) {
                                    AppLog.log(LogCategory.SYSTEM, "Hardware initialization timed out at 30 seconds.")
                                    CallManager.endCall()
                                    CallManager.markFailed(CallError.HARDWARE_ERROR)
                                    recordLocalCallFailure("Hardware Error")
                                    _callInitiationState.value = CallInitiationState.IDLE
                                }
                            }
                        }
                        CallInitiationResult.VALIDATION_FAILED -> {
                            _callInitiationState.value = CallInitiationState.IDLE
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun acceptInboundCall(context: Context) {
        _receiverConnectionState.value = ReceiverConnectionState.VALIDATING

        viewModelScope.launch(Dispatchers.IO) {
            val profile = AppGraph.appRepository.resolveActivePartnerProfile(CallManager.incomingCallerName)
            _profilePhotoPath.value = profile?.profilePhotoPath
        }

        CallManager.acceptIncomingCall(context)
        _receiverHardwareTimer.value = 0
        receiverTimerJob?.cancel()
        receiverTimerJob = viewModelScope.launch {
            while (_receiverHardwareTimer.value < 30 && (_receiverConnectionState.value == ReceiverConnectionState.VALIDATING || _receiverConnectionState.value == ReceiverConnectionState.INITIALIZING_HARDWARE)) {
                kotlinx.coroutines.delay(1000)
                _receiverHardwareTimer.value++
            }
            if (_receiverConnectionState.value == ReceiverConnectionState.VALIDATING) {
                AppLog.log(LogCategory.SYSTEM, "Validation timed out at 30 seconds.")
                CallManager.endCall()
                _receiverConnectionState.value = ReceiverConnectionState.FAILED_VALIDATING
            } else if (_receiverConnectionState.value == ReceiverConnectionState.INITIALIZING_HARDWARE) {
                AppLog.log(LogCategory.SYSTEM, "Hardware initialization timed out at 30 seconds.")
                CallManager.endCall()
                CallManager.markFailed(CallError.HARDWARE_ERROR)
                _receiverConnectionState.value = ReceiverConnectionState.FAILED_HARDWARE
            }
        }
    }

    fun declineInboundCall() {
        receiverTimerJob?.cancel()
        CallManager.declineIncomingCall()
        _receiverConnectionState.value = ReceiverConnectionState.IDLE
    }

    fun dismissReceiverState() {
        receiverTimerJob?.cancel()
        _receiverConnectionState.value = ReceiverConnectionState.IDLE
    }

    fun dismissCallError(): CallError {
        val err = CallManager.lastCallFailedDueToError.value
        CallManager.clearCallError()
        return err
    }

    val callHistory: StateFlow<List<CallHistoryNode>> = AppGraph.database.callHistoryDao()
        .getAllCalls()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    private val _undoDeleteEvent = kotlinx.coroutines.flow.MutableSharedFlow<CallHistoryNode>()
    val undoDeleteEvent: kotlinx.coroutines.flow.SharedFlow<CallHistoryNode> = _undoDeleteEvent

    private val _undoBulkDeleteEvent = kotlinx.coroutines.flow.MutableSharedFlow<List<CallHistoryNode>>()
    val undoBulkDeleteEvent: kotlinx.coroutines.flow.SharedFlow<List<CallHistoryNode>> = _undoBulkDeleteEvent

    fun clearCallHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = callHistory.value
            AppGraph.database.callHistoryDao().clearHistory()
            _undoBulkDeleteEvent.emit(currentList)
        }
    }

    fun deleteCall(node: CallHistoryNode) {
        viewModelScope.launch(Dispatchers.IO) {
            AppGraph.database.callHistoryDao().deleteCall(node)
            _undoDeleteEvent.emit(node)
        }
    }

    fun undoDeleteCall(node: CallHistoryNode) {
        viewModelScope.launch(Dispatchers.IO) {
            AppGraph.database.callHistoryDao().insertCall(node)
        }
    }

    fun undoClearCallHistory(nodes: List<CallHistoryNode>) {
        viewModelScope.launch(Dispatchers.IO) {
            AppGraph.database.callHistoryDao().insertCalls(nodes)
        }
    }

    fun toggleMute() { _isMuted.value = !_isMuted.value }
    fun toggleIncomingAudioMute() { _isIncomingAudioMuted.value = !_isIncomingAudioMuted.value }
    fun toggleVideo() { 
        _isVideoOn.value = !_isVideoOn.value 
        CallManager.setLocalVideoState(_isVideoOn.value)
    }
    fun toggleControls() { _isControlsVisible.value = !_isControlsVisible.value }
    fun toggleSwapVideo() { _isSwappedVideo.value = !_isSwappedVideo.value }
    fun setSwappedVideo(isSwapped: Boolean) { _isSwappedVideo.value = isSwapped }
    
    fun setRemoteVideo(isOn: Boolean) { 
        _isRemoteVideoOn.value = isOn 
        CallManager.setRemoteVideoState(isOn)
    }
    fun setLocalVideo(isOn: Boolean) { 
        _isVideoOn.value = isOn 
        CallManager.setLocalVideoState(isOn)
    }
    fun setRemoteAudioLevel(level: Float) { _remoteAudioLevel.value = level }

    fun onValidationPassed() {
        viewModelScope.launch {
            _validationPassedEvent.emit(Unit)
        }
    }

    private fun resetState() {
        _isMuted.value = false
        _isIncomingAudioMuted.value = false
        _isVideoOn.value = false
        _isRemoteVideoOn.value = false
        CallManager.setLocalVideoState(false)
        CallManager.setRemoteVideoState(false)
        _isControlsVisible.value = true
        _isSwappedVideo.value = false
        _remoteAudioLevel.value = 0f
        _profilePhotoPath.value = null
    }

    suspend fun initiateCall(context: Context): CallInitiationResult {
        resetState()
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val prefs = AppGraph.prefs
            val chat = prefs.activeChatId
            
            // Fetch cached profile image for the UI avatar
            val profile = AppGraph.appRepository.resolveActivePartnerProfile()
            _profilePhotoPath.value = profile?.profilePhotoPath

            val callUrls = CallManager.initCall(context)
            if (callUrls == null) {
                val err = CallManager.lastCallFailedDueToError.value
                val errText = if (err == CallError.NETWORK_ERROR) "Network Error" else "Config Error"
                recordLocalCallFailure(errText)
                return@withContext CallInitiationResult.VALIDATION_FAILED
            }
            
            val (_, telegramUrl) = callUrls
            currentTelegramUrl = telegramUrl
            
            CallInitiationResult.HARDWARE_INIT
        }
    }
    
    fun onHardwareReady() {
        viewModelScope.launch {
            _hardwareReadyEvent.emit(Unit)
        }
    }

    fun onError(errorMsg: String) {
        viewModelScope.launch {
            _errorEvent.emit(errorMsg)
        }
    }

    suspend fun sendTelegramLink(): CallInitiationResult {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val prefs = AppGraph.prefs
            val chat = prefs.activeChatId
            val token = prefs.botToken
            val telegramUrl = currentTelegramUrl
            
            if (token.isNotEmpty() && chat.isNotEmpty() && telegramUrl != null) {
                try {
                    val me = TelegramApi.getMe(token)
                    val botName = me?.result?.first_name ?: "Superiorchat"
                    val initiateTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())

                    val replyText = "===================\n" +
                                    "🔔 <b>New Call Incoming</b>\n" +
                                    "===================\n" +
                                    "<b>$botName is inviting you for call</b>\n\n" +
                                    "<b>Time</b> : $initiateTime\n\n" +
                                    "<b>Click Below Button To Join</b>"

                    val markup = InlineKeyboardMarkup(listOf(listOf(
                        InlineKeyboardButton(text = "🔰 Connect", url = telegramUrl),
                        InlineKeyboardButton(text = "❌ Decline Call", callbackData = "decline_call")
                    )))
                    val replyMarkup = TelegramApi.json.encodeToString(markup)
                    
                    if (CallManager.callState.value != CallState.IDLE) {
                        val msgId = TelegramApi.sendMessage(
                            token = token,
                            chatId = chat,
                            text = replyText,
                            parseMode = "HTML",
                            replyMarkup = replyMarkup
                        )
                        
                        if (msgId != null) {
                            if (CallManager.callState.value == CallState.CONNECTING) {
                                CallManager.startTimeout()
                            }
                            val now = System.currentTimeMillis()
                            val localEventMsgId: Long
                            // Ensure conversation row exists before inserting any message (FK requirement).
                            AppGraph.appRepository.ensureConversationExists(chat)
                            if (now - lastCallEventTime < 15000 && lastCallEventMsgId != 0L) {
                                localEventMsgId = lastCallEventMsgId
                                AppGraph.appRepository.updateMessageText(localEventMsgId, "Outgoing Call")
                            } else {
                                localEventMsgId = -now
                                lastCallEventMsgId = localEventMsgId
                                lastCallEventTime = now
                                val eventNode = com.mobile.superiorchat.data.entity.MessageNode(
                                    messageId = localEventMsgId,
                                    conversationId = chat,
                                    senderId = "ME",
                                    text = "Outgoing Call",
                                    timestamp = now,
                                    isFromMe = true,
                                    mediaType = "call_event",
                                    status = com.mobile.superiorchat.data.entity.MessageStatus.SENT
                                )
                                AppGraph.appRepository.insertMessage(eventNode)
                            }
                            monitorCallLifecycle(token, chat, msgId, botName, localEventMsgId)
                            CallInitiationResult.SUCCESS
                        } else {
                            AppLog.log(LogCategory.ERROR, "Failed to deliver call link to Telegram.")
                            CallManager.endCall()
                            CallInitiationResult.TELEGRAM_FAILED
                        }
                    } else {
                        CallInitiationResult.VALIDATION_FAILED
                    }
                } catch (e: Exception) {
                    AppLog.log(LogCategory.ERROR, "Failed to send call link: ${e.message}")
                    CallManager.endCall()
                    CallInitiationResult.TELEGRAM_FAILED
                }
            } else {
                CallInitiationResult.VALIDATION_FAILED
            }
        }
    }

    private fun monitorCallLifecycle(token: String, chatId: String, messageId: Long, botName: String, localEventMsgId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Observe when call transitions to ACTIVE (Caller has joined)
                val activeJob = launch {
                    CallManager.callState.first { it == CallState.ACTIVE }
                    try {
                        val connectedTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                        val ongoingText = "===================\n" +
                                          "📞 *Call in Progress*\n" +
                                          "===================\n" +
                                          "*Your call is now in progress*\n\n" +
                                          "*Started at* : $connectedTime"
                        
                        val emptyMarkup = TelegramApi.json.encodeToString(InlineKeyboardMarkup(emptyList()))
                        
                        TelegramApi.editMessageText(
                            token = token,
                            chatId = chatId,
                            messageId = messageId,
                            text = ongoingText,
                            parseMode = "Markdown",
                            replyMarkup = emptyMarkup
                        )
                        AppLog.log(LogCategory.SYSTEM, "Telegram call message updated to Call in Progress (Buttons stripped)")
                    } catch (e: Exception) {
                        AppLog.log(LogCategory.ERROR, "Failed to update Telegram message on call connect: ${e.message}")
                    }
                }

                // 2. Observe when call transitions to ENDING / IDLE
                CallManager.callState.first { it == CallState.ENDING || it == CallState.IDLE }
                activeJob.cancel()
                
                val duration = if (CallManager.callDuration.value > 0) CallManager.callDuration.value else CallManager.lastCompletedDuration
                val isMissed = duration == 0L
                val lastError = CallManager.lastCallFailedDueToError.value
                val domain = CallManager.currentBaseUrl ?: ""
                val peerJsId = CallManager.currentRoomId ?: ""
                
                val status = when {
                    duration > 0L -> "COMPLETED"
                    lastError == com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "FAILED_NETWORK"
                    lastError == com.mobile.superiorchat.core.call.CallError.NO_ANSWER -> "FAILED_NO_ANSWER"
                    lastError == com.mobile.superiorchat.core.call.CallError.HARDWARE_ERROR -> "FAILED_HARDWARE"
                    lastError == com.mobile.superiorchat.core.call.CallError.INVALID_URL -> "FAILED_CONFIG"
                    lastError == com.mobile.superiorchat.core.call.CallError.DECLINED -> "DECLINED"
                    else -> "CANCELLED"
                }

                val endTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                
                // Save Call History
                val prefs = AppGraph.prefs
                val profile = AppGraph.database.profileDao().getProfileSync(chatId)
                val partnerName = if (prefs.isPeerLinkEnabled) {
                    val targetBot = prefs.peerLinkPartnerBotUsername.trim().removePrefix("@")
                    val cachedBotProfile = if (targetBot.isNotBlank()) {
                        AppGraph.database.profileDao().getAllProfilesSync().firstOrNull { it.username.equals(targetBot, ignoreCase = true) }
                    } else null
                    cachedBotProfile?.title?.takeIf { it.isNotBlank() }
                        ?: if (targetBot.isNotBlank()) "@$targetBot"
                        else profile?.title?.takeIf { it.isNotBlank() } ?: "Partner"
                } else {
                    profile?.title?.takeIf { it.isNotBlank() } ?: "Partner"
                }

                val node = com.mobile.superiorchat.data.entity.CallHistoryNode(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    isMissed = isMissed,
                    isIncoming = false,
                    callStatus = status,
                    peerJsId = peerJsId,
                    domain = domain,
                    partnerName = partnerName
                )
                AppGraph.database.callHistoryDao().insertCall(node)

                val localText = if (isMissed) {
                    when (lastError) {
                        com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "Network Error"
                        com.mobile.superiorchat.core.call.CallError.NO_ANSWER -> "Unanswered Call"
                        com.mobile.superiorchat.core.call.CallError.DECLINED -> "Call Declined"
                        else -> "Call Cancelled"
                    }
                } else {
                    lastCallEventMsgId = 0L // Clear reference so next call doesn't overwrite a completed call
                    "Call Ended - ${CallManager.formatDurationText(duration)}"
                }
                AppGraph.appRepository.updateMessageText(localEventMsgId, localText)

                val header = if (lastError == com.mobile.superiorchat.core.call.CallError.NONE) "❌ *Call Cancelled*" 
                             else if (lastError == com.mobile.superiorchat.core.call.CallError.DECLINED) "❌ *Call Declined*"
                             else "❌ *Call Missed*"
                val detail = when (lastError) {
                    com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "*Call Failed due to Network Error*"
                    com.mobile.superiorchat.core.call.CallError.DECLINED -> "*Call was declined by receiver*"
                    else -> "*$botName tried connecting with you*"
                }

                val updatedText = if (isMissed) {
                    "===================\n" +
                    "$header\n" +
                    "===================\n" +
                    "$detail\n\n" +
                    "*Time* : $endTime"
                } else {
                    "===================\n" +
                    "💖 *Call Ended*\n" +
                    "===================\n" +
                    "*Your call has been ended*\n\n" +
                    "*Time* : $endTime\n" +
                    "*Elapsed* : ${CallManager.formatDurationText(duration)}"
                }
                
                val emptyMarkup = TelegramApi.json.encodeToString(InlineKeyboardMarkup(emptyList()))
                
                TelegramApi.editMessageText(
                    token = token,
                    chatId = chatId,
                    messageId = messageId,
                    text = updatedText,
                    parseMode = "Markdown",
                    replyMarkup = emptyMarkup
                )
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to update Telegram message on call end: ${e.message}")
            }
        }
    }

    fun recordLocalCallFailure(errorText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chat = AppGraph.prefs.activeChatId
            if (chat.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val lastError = CallManager.lastCallFailedDueToError.value

            // 1. Always save to call_history — no FK, always safe regardless of DB state.
            val callStatus = when (lastError) {
                CallError.NETWORK_ERROR -> "FAILED_NETWORK"
                CallError.HARDWARE_ERROR -> "FAILED_HARDWARE"
                CallError.INVALID_URL -> "FAILED_CONFIG"
                else -> "FAILED_CONFIG"
            }
            val profile = AppGraph.appRepository.resolveActivePartnerProfile()
            val partnerName = profile?.title?.takeIf { it.isNotBlank() }
                ?: if (AppGraph.prefs.peerLinkPartnerBotUsername.isNotBlank()) "@${AppGraph.prefs.peerLinkPartnerBotUsername.removePrefix("@")}"
                else "Partner"

            AppGraph.database.callHistoryDao().insertCall(
                CallHistoryNode(
                    timestamp = now,
                    durationSeconds = 0L,
                    isMissed = true,
                    isIncoming = false,
                    callStatus = callStatus,
                    peerJsId = CallManager.currentRoomId ?: "",
                    domain = CallManager.currentBaseUrl ?: "",
                    partnerName = partnerName
                )
            )

            // 2. Ensure conversation row exists, then insert the chat bubble event.
            //    ensureConversationExists uses INSERT OR IGNORE — safe to call even if conversation
            //    already exists. This guarantees the FK is always satisfied before insertMessage.
            AppGraph.appRepository.ensureConversationExists(chat)
            val localEventMsgId = -now
            val eventNode = com.mobile.superiorchat.data.entity.MessageNode(
                messageId = localEventMsgId,
                conversationId = chat,
                senderId = "ME",
                text = errorText,
                timestamp = now,
                isFromMe = true,
                mediaType = "call_event",
                status = com.mobile.superiorchat.data.entity.MessageStatus.SENT
            )
            AppGraph.appRepository.insertMessage(eventNode)
        }
    }
}
