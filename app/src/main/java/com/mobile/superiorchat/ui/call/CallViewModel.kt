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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.mobile.superiorchat.data.entity.CallHistoryNode

enum class CallInitiationResult { SUCCESS, VALIDATION_FAILED, TELEGRAM_FAILED, HARDWARE_INIT }

class CallViewModel : ViewModel() {

    private var lastCallEventMsgId: Long = 0L
    private var lastCallEventTime: Long = 0L

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoOn = MutableStateFlow(false)
    val isVideoOn: StateFlow<Boolean> = _isVideoOn.asStateFlow()

    private val _isRemoteVideoOn = MutableStateFlow(false)
    val isRemoteVideoOn: StateFlow<Boolean> = _isRemoteVideoOn.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    private val _hardwareReadyEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val hardwareReadyEvent = _hardwareReadyEvent.asSharedFlow()

    private var currentTelegramUrl: String? = null

    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isSwappedVideo = MutableStateFlow(false)
    val isSwappedVideo: StateFlow<Boolean> = _isSwappedVideo.asStateFlow()

    private val _remoteAudioLevel = MutableStateFlow(0f)
    val remoteAudioLevel: StateFlow<Float> = _remoteAudioLevel.asStateFlow()

    private val _profilePhotoPath = MutableStateFlow<String?>(null)
    val profilePhotoPath: StateFlow<String?> = _profilePhotoPath.asStateFlow()

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

    private fun resetState() {
        _isMuted.value = false
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
            val chat = prefs.chatId
            
            // Fetch cached profile image for the UI avatar
            val profile = AppGraph.database.profileDao().getProfileSync(chat)
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

    suspend fun sendTelegramLink(): CallInitiationResult {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val prefs = AppGraph.prefs
            val chat = prefs.chatId
            val token = prefs.botToken
            val telegramUrl = currentTelegramUrl
            
            if (token.isNotEmpty() && chat.isNotEmpty() && telegramUrl != null) {
                try {
                    val me = TelegramApi.getMe(token)
                    val botName = me?.result?.first_name ?: "Superiorchat"
                    val initiateTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())

                    val replyText = "===================\n" +
                                    "🔔 *New Call Incoming*\n" +
                                    "===================\n" +
                                    "*$botName is inviting you for call*\n\n" +
                                    "*Time* : $initiateTime\n\n" +
                                    "*Click Below Button To Join*"
                    
                    val markup = InlineKeyboardMarkup(listOf(listOf(InlineKeyboardButton(text = "🔰 Connect", url = telegramUrl))))
                    val replyMarkup = TelegramApi.json.encodeToString(markup)
                    
                    if (CallManager.callState.value != CallState.IDLE) {
                        val msgId = TelegramApi.sendMessage(
                            token = token,
                            chatId = chat,
                            text = replyText,
                            parseMode = "Markdown",
                            replyMarkup = replyMarkup
                        )
                        
                        if (msgId != null) {
                            if (CallManager.callState.value == CallState.CONNECTING) {
                                CallManager.startTimeout()
                            }
                            val now = System.currentTimeMillis()
                            val localEventMsgId: Long
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
                            monitorCallTermination(token, chat, msgId, botName, localEventMsgId)
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

    private fun monitorCallTermination(token: String, chatId: String, messageId: Long, botName: String, localEventMsgId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                CallManager.callState.first { it == CallState.ENDING || it == CallState.IDLE }
                
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
                    else -> "CANCELLED"
                }

                val endTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                
                // Save Call History
                val profile = AppGraph.database.profileDao().getProfileSync(chatId)
                val partnerName = profile?.title ?: "Unknown"

                val node = CallHistoryNode(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    isMissed = isMissed,
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
                        else -> "Call Cancelled"
                    }
                } else {
                    lastCallEventMsgId = 0L // Clear reference so next call doesn't overwrite a completed call
                    "Call Ended - ${CallManager.formatDurationText(duration)}"
                }
                AppGraph.appRepository.updateMessageText(localEventMsgId, localText)

                val header = if (lastError == com.mobile.superiorchat.core.call.CallError.NONE) "❌ *Call Cancelled*" else "❌ *Call Missed*"
                val detail = when (lastError) {
                    com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "*Call Failed due to Network Error*"
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
            val chat = AppGraph.prefs.chatId
            if (chat.isEmpty()) return@launch
            
            val now = System.currentTimeMillis()
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
