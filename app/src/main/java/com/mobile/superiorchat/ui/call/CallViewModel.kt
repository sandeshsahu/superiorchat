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

enum class CallInitiationResult { SUCCESS, VALIDATION_FAILED, TELEGRAM_FAILED, HARDWARE_INIT }

class CallViewModel : ViewModel() {

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

            val callUrls = CallManager.initCall(context) ?: return@withContext CallInitiationResult.VALIDATION_FAILED
            
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
                    val botName = me?.result?.first_name ?: "SuperiorChat"
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
                            monitorCallTermination(token, chat, msgId, botName)
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

    private fun monitorCallTermination(token: String, chatId: String, messageId: Long, botName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                CallManager.callState.first { it == CallState.ENDING || it == CallState.IDLE }
                
                val duration = CallManager.callDuration.value
                val isMissed = duration == 0L
                val endTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                
                val updatedText = if (isMissed) {
                    "===================\n" +
                    "❌ *Call Missed*\n" +
                    "===================\n" +
                    "*$botName tried connecting with you*\n\n" +
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
}
