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

class CallViewModel : ViewModel() {

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoOn = MutableStateFlow(false)
    val isVideoOn: StateFlow<Boolean> = _isVideoOn.asStateFlow()

    private val _isRemoteVideoOn = MutableStateFlow(false)
    val isRemoteVideoOn: StateFlow<Boolean> = _isRemoteVideoOn.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isSwappedVideo = MutableStateFlow(false)
    val isSwappedVideo: StateFlow<Boolean> = _isSwappedVideo.asStateFlow()

    fun toggleMute() { _isMuted.value = !_isMuted.value }
    fun toggleVideo() { _isVideoOn.value = !_isVideoOn.value }
    fun toggleControls() { _isControlsVisible.value = !_isControlsVisible.value }
    fun toggleSwapVideo() { _isSwappedVideo.value = !_isSwappedVideo.value }
    
    fun setRemoteVideo(isOn: Boolean) { _isRemoteVideoOn.value = isOn }
    fun setLocalVideo(isOn: Boolean) { _isVideoOn.value = isOn }

    fun initiateCall(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val (vercelUrl, telegramUrl) = CallManager.initCall(context)
            val prefs = AppGraph.prefs
            val token = prefs.botToken
            val chat = prefs.chatId
            if (token.isNotEmpty() && chat.isNotEmpty()) {
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
                        } else {
                            AppLog.log(LogCategory.ERROR, "Failed to deliver call link to Telegram.")
                            StatusFlow.reportStatus(SyncState.ERROR, "Failed to send link")
                            CallManager.endCall()
                        }
                    }
                } catch (e: Exception) {
                    AppLog.log(LogCategory.ERROR, "Failed to send call link: ${e.message}")
                    CallManager.endCall()
                }
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
