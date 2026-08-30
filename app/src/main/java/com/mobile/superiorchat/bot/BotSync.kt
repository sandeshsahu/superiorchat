package com.mobile.superiorchat.bot

import android.content.Context
import com.mobile.superiorchat.core.NetState
import com.mobile.superiorchat.core.AppGraph

import com.mobile.superiorchat.data.entity.ChatNode
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.mobile.superiorchat.media.MediaSync
import com.mobile.superiorchat.media.LocalDirs
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState

class BotSync(private val context: Context) {

    private val prefs = AppGraph.prefs
    private val repository = AppGraph.appRepository
    val notifier = Notifier(context, CoroutineScope(Dispatchers.IO))

    private var pollingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastUpdateId: Long
        get() = prefs.lastUpdateId
        set(value) {
            prefs.lastUpdateId = value
        }
    private var networkCollectorJob: Job? = null
    private val networkWakeChannel = Channel<Unit>(Channel.CONFLATED)

    private var showSyncFeedback = true // Flag to control sync feedback spam
    @Volatile private var isNetworkAvailable = false

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        registerNetworkCallback()
        launchPollingLoop()
        flushQueuedMessages()
        syncTargetProfile()
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        unregisterNetworkCallback()
        AppLog.log(LogCategory.SYSTEM, "Bot polling stopped.")
    }

    // -- Network-Aware Polling --

    private fun registerNetworkCallback() {
        if (networkCollectorJob != null) return
        networkCollectorJob = coroutineScope.launch {
            launch {
                NetState.isOnline.collect { isOnline ->
                    isNetworkAvailable = isOnline
                    
                    if (isOnline) {
                        // Optimistically assume API is reachable to hide the 10-second DNS/TLS connection delay
                        // from snoopers. If it actually fails, the catch block will set it to false later.
                        AppLog.setTelegramApiReachable(true)
                        
                        if (prefs.botToken.isBlank()) {
                            StatusFlow.reportStatus(SyncState.AUTH_ERROR, "Credentials empty")
                        } else if (StatusFlow.syncState.value == SyncState.OFFLINE) {
                            StatusFlow.reportStatus(SyncState.SUCCESS, "Online")
                        }
                        AppLog.log(LogCategory.SYSTEM, "Network available. Waking up polling loop and flushing queue.")
                        networkWakeChannel.trySend(Unit)
                        flushQueuedMessages()
                    } else {
                        StatusFlow.reportStatus(SyncState.OFFLINE, "Connection offline")
                        AppLog.log(LogCategory.SYSTEM, "Network lost. Canceling active requests and pausing polling.")
                        TelegramApi.client.dispatcher.cancelAll()
                        // We intentionally leave isTelegramApiReachable alone here.
                        showSyncFeedback = true
                    }
                    
                    // Update notifier with the new state
                    notifier.setNetworkState(isNetworkAvailable, AppLog.isTelegramApiReachable.value)
                }
            }
            launch {
                AppLog.isTelegramApiReachable.collect { isReachable ->
                    notifier.setNetworkState(isNetworkAvailable, isReachable)
                    if (!isReachable && isNetworkAvailable) {
                        if (prefs.botToken.isBlank()) {
                            StatusFlow.reportStatus(SyncState.AUTH_ERROR, "Credentials empty")
                        } else {
                            StatusFlow.reportStatus(SyncState.OFFLINE, "Telegram API Unreachable")
                        }
                        showSyncFeedback = true
                    } else if (isReachable && isNetworkAvailable) {
                        if (prefs.botToken.isBlank()) {
                            StatusFlow.reportStatus(SyncState.AUTH_ERROR, "Credentials empty")
                        } else if (StatusFlow.syncState.value == SyncState.OFFLINE) {
                            StatusFlow.reportStatus(SyncState.SUCCESS, "Online")
                        }
                        AppLog.log(LogCategory.SYSTEM, "API reachable. Waking up polling loop.")
                        networkWakeChannel.trySend(Unit)
                        flushQueuedMessages()
                    }
                }
            }
        }
    }

    private fun unregisterNetworkCallback() {
        networkCollectorJob?.cancel()
        networkCollectorJob = null
    }

    private fun launchPollingLoop() {
        pollingJob = coroutineScope.launch {
            AppLog.log(LogCategory.SYSTEM, "Bot polling started.")
            var consecutiveFailures = 0

            while (isActive) {
                if (!isNetworkAvailable) {
                    AppLog.log(LogCategory.SYSTEM, "Network offline. Polling paused completely.")
                    AppLog.setTelegramApiReachable(false)
                    networkWakeChannel.receive() // Suspend indefinitely until onAvailable
                    continue
                }

                var hasUpdates = false
                try {
                    val token = prefs.botToken

                    if (token.isEmpty()) {
                        AppLog.log(LogCategory.SYSTEM, "Bot token is empty. Pausing polling.")
                        delay(30000)
                        continue
                    }

                    TelegramApi.getUpdatesRaw(token, lastUpdateId + 1, 80).use { response ->
                        if (response.isSuccessful) {
                            consecutiveFailures = 0
                            AppLog.setTelegramApiReachable(true)

                            val body = response.body?.string()
                            if (!body.isNullOrEmpty()) {
                                val updateResponse = TelegramApi.json.decodeFromString<UpdateResponse>(body)
                                if (updateResponse.ok) {
                                    hasUpdates = updateResponse.result.isNotEmpty()
                                    if (hasUpdates && showSyncFeedback) {
                                        StatusFlow.reportStatus(SyncState.SYNCING_MESSAGES, "Syncing new messages...")
                                    }
                                    for (update in updateResponse.result) {
                                        try {
                                            lastUpdateId = update.update_id
                                            handleUpdate(update)
                                        } catch (e: Exception) {
                                            AppLog.log(LogCategory.SYSTEM, "Failed to handle update ${update.update_id}: ${e.message}", LogLevel.ERROR)
                                        }
                                    }
                                    if (hasUpdates && showSyncFeedback) {
                                        val count = updateResponse.result.size
                                        StatusFlow.reportStatus(SyncState.SUCCESS, "Synced $count message${if(count > 1) "s" else ""}")
                                    }
                                    showSyncFeedback = false // Silence future continuous polling
                                }
                            }
                        } else {
                            if (response.code == 409) {
                                consecutiveFailures += 2 // Give other instances time to die
                            } else if (response.code == 401) {
                                AppLog.setBotTokenInvalid(true)
                                StatusFlow.reportStatus(SyncState.AUTH_ERROR, "Invalid Bot Token")
                                consecutiveFailures += 5 // Backoff strongly
                            } else {
                                consecutiveFailures++
                            }
                            val errorBody = response.body?.string()
                            AppLog.log(LogCategory.NETWORK, "Polling failed: ${response.code} - $errorBody", LogLevel.ERROR)
                        }
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                        consecutiveFailures += 5 // Fast forward backoff for completely unreachable network
                    } else {
                        consecutiveFailures++
                    }
                    AppLog.log(LogCategory.NETWORK, "Polling exception: ${e.message}", LogLevel.ERROR)
                    AppLog.setTelegramApiReachable(false)
                }

                // Exponential backoff: 333ms base, caps at 5 minutes after repeated failures
                val backoffMs = if (consecutiveFailures > 0) {
                    minOf(333L * (1L shl minOf(consecutiveFailures, 10)), 300000L)
                } else {
                    if (hasUpdates) 0L else 1500L
                }
                
                if (backoffMs > 0L) {
                    // Wait for backoff, or wake up instantly if network becomes available
                    kotlinx.coroutines.withTimeoutOrNull<Unit>(backoffMs) {
                        networkWakeChannel.receive()
                    }
                }
            }
        }
    }

    private suspend fun handleUpdate(update: Update) {
        val token = prefs.botToken
        val myBotId = prefs.myBotId

        // Drop any reflected messages or edits sent by our own bot in group chats to prevent self-duplication
        if (myBotId.isNotEmpty()) {
            if (update.message?.from?.id?.toString() == myBotId) return
            if (update.edited_message?.from?.id?.toString() == myBotId) return
            if (update.callback_query?.from?.id?.toString() == myBotId) return
        }

        if (update.callback_query != null) {
            val query = update.callback_query
            if (query.data == "decline_call") {
                val queryChatId = query.message?.chat?.id?.toString() ?: ""
                val isGroup = com.mobile.superiorchat.utils.Validator.isValidGroupChatId(prefs.activeChatId)

                // Verify callback origin belongs to the active conversation
                val isAuthorizedCallback = if (isGroup) {
                    queryChatId == prefs.activeChatId
                } else {
                    query.from.id.toString() == prefs.activeChatId
                }

                if (!isAuthorizedCallback) {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Ignored decline callback from unauthorized chat/user: ${query.from.id}", LogLevel.WARN)
                    return
                }

                val callState = com.mobile.superiorchat.core.call.CallManager.callState.value

                if (callState == com.mobile.superiorchat.core.call.CallState.CONNECTING || 
                    callState == com.mobile.superiorchat.core.call.CallState.RINGING) {
                    // Only decline if the call is still ringing or connecting
                    com.mobile.superiorchat.core.call.CallManager.markFailed(com.mobile.superiorchat.core.call.CallError.DECLINED)
                    
                    if (token.isNotBlank()) {
                        TelegramApi.answerCallbackQuery(token, query.id, text = "Call Declined", showAlert = false)
                        
                        query.message?.let { msg ->
                            val endTime = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                            val formattedText = "===================\n" +
                                                "❌ *Call Declined*\n" +
                                                "===================\n" +
                                                "*Call was declined by receiver*\n\n" +
                                                "*Time* : $endTime"
                                                
                            TelegramApi.editMessageText(
                                token = token,
                                chatId = msg.chat.id.toString(),
                                messageId = msg.message_id,
                                text = formattedText,
                                parseMode = "Markdown",
                                replyMarkup = TelegramApi.json.encodeToString(com.mobile.superiorchat.bot.InlineKeyboardMarkup(emptyList()))
                            )
                            // Update local DB to make it render as a missed call natively
                            repository.updateMessageText(msg.message_id, "Call Declined")
                        }
                    }
                } else if (callState == com.mobile.superiorchat.core.call.CallState.ACTIVE) {
                    // Call is already connected and ongoing — ignore decline and inform the user
                    if (token.isNotBlank()) {
                        TelegramApi.answerCallbackQuery(token, query.id, text = "Call is already in progress", showAlert = false)
                    }
                } else {
                    // Call is already IDLE or ENDING
                    if (token.isNotBlank()) {
                        TelegramApi.answerCallbackQuery(token, query.id, text = "Call already ended", showAlert = false)
                    }
                }
            }
            return
        }

        if (update.edited_message != null) {
            val editedMsg = update.edited_message
            val rawText = editedMsg.text ?: editedMsg.caption
            if (rawText != null) {
                // Reject raw system signals in edited messages to prevent DB pollution
                val signal = com.mobile.superiorchat.utils.Validator.extractSystemSignal(rawText)
                if (signal !is com.mobile.superiorchat.utils.Validator.SystemSignal.None) {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Dropped system signal in edited_message: $rawText")
                    return
                }

                var text = rawText
                val existingMsg = repository.getMessageById(editedMsg.message_id)
                if (existingMsg?.mediaType == "call_event" && prefs.isPeerLinkEnabled) {
                    // Check if the caller aborted the call
                    if (com.mobile.superiorchat.core.call.CallManager.isIncomingCall && 
                        com.mobile.superiorchat.core.call.CallManager.incomingTelegramMsgId == editedMsg.message_id) {
                        if (text.contains("Call Cancelled") || text.contains("Call Ended") || text.contains("Call Missed")) {
                            AppLog.log(LogCategory.BOT_ACTIVITY, "Caller aborted the call. Ending local ringing.")
                            com.mobile.superiorchat.core.call.CallManager.markFailed(com.mobile.superiorchat.core.call.CallError.NONE)
                        }
                    }
                    return // Ignore edited message from Telegram; we manage call UI locally
                }
                
                val entities = editedMsg.entities ?: editedMsg.caption_entities
                val formattedEditedText = com.mobile.superiorchat.utils.Validator.applyTelegramEntities(text, entities)
                val editTime = (editedMsg.edit_date ?: editedMsg.date) * 1000L
                repository.updateMessageText(editedMsg.message_id, formattedEditedText, editTime)
                AppLog.log(LogCategory.BOT_ACTIVITY, "Updated edited message: ${formattedEditedText.take(50)}")
            }
            return
        }


        if (update.message_reaction != null) {
            val reactionUpdate = update.message_reaction
            val myBotId = prefs.myBotId
            val reactorId = reactionUpdate.user?.id?.toString()
            val isMe = myBotId.isNotBlank() && reactorId == myBotId
            val emojis = reactionUpdate.new_reaction.mapNotNull { it.emoji }

            val existingMsg = repository.getMessageById(reactionUpdate.message_id)
            val currentData = com.mobile.superiorchat.data.entity.ReactionData.parse(existingMsg?.reactions)
            val newData = if (isMe) {
                currentData.copy(me = emojis)
            } else {
                currentData.copy(peer = emojis, peerSenderId = reactorId ?: currentData.peerSenderId)
            }
            val newJson = com.mobile.superiorchat.data.entity.ReactionData.toJson(newData)
            repository.updateMessageReactions(reactionUpdate.message_id, newJson)
            AppLog.log(LogCategory.BOT_ACTIVITY, "Reaction updated on msg ${reactionUpdate.message_id}: $newJson")

            // Sync profile for reactor if peer and not self (silent background sync if not cached)
            if (!isMe && reactorId != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val cached = repository.getProfileSync(reactorId)
                    if (cached == null || cached.profilePhotoPath.isBlank() || !java.io.File(cached.profilePhotoPath).exists()) {
                        MediaSync.syncSenderProfile(context, token, reactorId, reactionUpdate.user?.first_name, reactionUpdate.user?.username, silent = true)
                    }
                }
            }
            return
        }

        val message = update.message ?: return

        // Intruder filtering: only accept messages from the target chat
        val chatId = message.chat.id.toString()
        val senderId = message.from?.id?.toString() ?: ""

        if (prefs.isPeerLinkEnabled) {
            val isAuthorized = com.mobile.superiorchat.utils.Validator.isAuthorizedPeerLinkMessage(
                msgChatId = chatId,
                fromUsername = message.from?.username,
                isBot = message.from?.is_bot,
                activeChatId = prefs.activeChatId,
                partnerBotUsername = prefs.peerLinkPartnerBotUsername,
                isPeerLinkEnabled = true
            )
            if (!isAuthorized) {
                // If partner bot username was not filled yet, allow message from group if matching activeChatId
                val isGroupChat = com.mobile.superiorchat.utils.Validator.isValidGroupChatId(prefs.activeChatId) && chatId == prefs.activeChatId
                if (!isGroupChat) {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Intruder detected in PeerLink! Ignored msg from chat $chatId user ${message.from?.username}", LogLevel.WARN)
                    return
                }
                if (prefs.peerLinkPartnerBotUsername.isNotBlank()) {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Intruder detected in PeerLink! Ignored msg from user ${message.from?.username} (expected ${prefs.peerLinkPartnerBotUsername})", LogLevel.WARN)
                    return
                }
            }
        } else {
            val targetChatId = prefs.activeChatId
            if (targetChatId.isEmpty() || chatId != targetChatId) {
                AppLog.log(LogCategory.BOT_ACTIVITY, "Intruder detected! Ignored message from chat $chatId", LogLevel.WARN)
                return
            }
        }

        var text = message.text ?: message.caption ?: ""
        
        // Core Interceptor: Parse & Execute internal system signals before DB insertion or UI notifications
        val systemSignal = com.mobile.superiorchat.utils.Validator.extractSystemSignal(text)
        if (systemSignal !is com.mobile.superiorchat.utils.Validator.SystemSignal.None) {
            val isSignalAuthorized = com.mobile.superiorchat.utils.Validator.isAuthorizedSignalSender(
                msgChatId = chatId,
                fromUser = message.from,
                activeChatId = prefs.activeChatId,
                partnerBotUsername = prefs.peerLinkPartnerBotUsername,
                myBotId = myBotId
            )

            if (!isSignalAuthorized) {
                AppLog.log(LogCategory.BOT_ACTIVITY, "Unauthorized system signal rejected from chat $chatId user ${message.from?.username} (id=${message.from?.id})", LogLevel.WARN)
                return // Core-level drop
            }

            when (systemSignal) {
                is com.mobile.superiorchat.utils.Validator.SystemSignal.DeleteMessages -> {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Received authorized delete signal for ${systemSignal.messageIds.size} messages: ${systemSignal.messageIds}")
                    coroutineScope.launch(Dispatchers.IO) {
                        repository.deleteMessages(chatId, systemSignal.messageIds)
                        // Clean up the transient signal message from Telegram (receiver scrubs it)
                        if (token.isNotBlank()) {
                            TelegramApi.deleteMessage(token, chatId, message.message_id)
                        }
                    }
                }
                is com.mobile.superiorchat.utils.Validator.SystemSignal.CallDeclined -> {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Received authorized PeerLink decline signal")
                    coroutineScope.launch(Dispatchers.IO) {
                        message.reply_to_message?.message_id?.let { repliedMsgId ->
                            val existing = repository.getMessageById(repliedMsgId)
                            if (existing != null && existing.mediaType == "call_event") {
                                repository.updateMessageText(repliedMsgId, "Call Declined")
                                AppLog.log(LogCategory.BOT_ACTIVITY, "Retroactively updated call event message $repliedMsgId to Declined")
                            }
                        }
                        if (com.mobile.superiorchat.core.call.CallManager.callState.value != com.mobile.superiorchat.core.call.CallState.IDLE) {
                            com.mobile.superiorchat.core.call.CallManager.markFailed(com.mobile.superiorchat.core.call.CallError.DECLINED)
                        }
                        // Clean up the transient signal message from Telegram (receiver scrubs it)
                        if (token.isNotBlank()) {
                            TelegramApi.deleteMessage(token, chatId, message.message_id)
                        }
                    }
                }
                is com.mobile.superiorchat.utils.Validator.SystemSignal.BatchReactionUpdate -> {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Received authorized PeerLink batch reaction signal: ${systemSignal.updates.size} updates")
                    coroutineScope.launch(Dispatchers.IO) {
                        val senderId = message.from?.id?.toString()
                        systemSignal.updates.forEach { (msgId, emoji) ->
                            val existingMsg = repository.getMessageById(msgId)
                            if (existingMsg != null) {
                                val currentData = com.mobile.superiorchat.data.entity.ReactionData.parse(existingMsg.reactions)
                                val newPeer = if (emoji.isBlank()) emptyList() else listOf(emoji)
                                val newData = currentData.copy(peer = newPeer, peerSenderId = senderId ?: currentData.peerSenderId)
                                val newJson = com.mobile.superiorchat.data.entity.ReactionData.toJson(newData)
                                repository.updateMessageReactions(msgId, newJson)
                            }
                        }
                        // Sync profile for reaction sender once for the batch (silent background sync if not cached)
                        if (senderId != null) {
                            val cached = repository.getProfileSync(senderId)
                            if (cached == null || cached.profilePhotoPath.isBlank() || !java.io.File(cached.profilePhotoPath).exists()) {
                                MediaSync.syncSenderProfile(context, token, senderId, message.from?.first_name, message.from?.username, silent = true)
                            }
                        }
                        // Clean up the transient signal message from Telegram (receiver scrubs it)
                        if (token.isNotBlank()) {
                            TelegramApi.deleteMessage(token, chatId, message.message_id)
                        }
                    }
                }
                is com.mobile.superiorchat.utils.Validator.SystemSignal.ReactionUpdate -> {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Received authorized PeerLink reaction signal on msg ${systemSignal.messageId}: emoji='${systemSignal.emoji}', isClear=${systemSignal.isClear}")
                    coroutineScope.launch(Dispatchers.IO) {
                        val existingMsg = repository.getMessageById(systemSignal.messageId)
                        val senderId = message.from?.id?.toString()
                        if (existingMsg != null) {
                            val currentData = com.mobile.superiorchat.data.entity.ReactionData.parse(existingMsg.reactions)
                            val newPeer = if (systemSignal.isClear) emptyList() else listOf(systemSignal.emoji)
                            val newData = currentData.copy(peer = newPeer, peerSenderId = senderId ?: currentData.peerSenderId)
                            val newJson = com.mobile.superiorchat.data.entity.ReactionData.toJson(newData)
                            repository.updateMessageReactions(systemSignal.messageId, newJson)
                            AppLog.log(LogCategory.BOT_ACTIVITY, "Updated reactions on msg ${systemSignal.messageId}: $newJson")
                        }
                        // Sync profile for reaction sender (silent background sync if not cached)
                        if (senderId != null) {
                            val cached = repository.getProfileSync(senderId)
                            if (cached == null || cached.profilePhotoPath.isBlank() || !java.io.File(cached.profilePhotoPath).exists()) {
                                MediaSync.syncSenderProfile(context, token, senderId, message.from?.first_name, message.from?.username, silent = true)
                            }
                        }
                        // Clean up the transient signal message from Telegram (receiver scrubs it)
                        if (token.isNotBlank()) {
                            TelegramApi.deleteMessage(token, chatId, message.message_id)
                        }
                    }
                }
                is com.mobile.superiorchat.utils.Validator.SystemSignal.Unknown -> {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "Received future/unknown system signal: ${systemSignal.rawSignal}")
                }
                com.mobile.superiorchat.utils.Validator.SystemSignal.None -> {}
            }
            return // Core-level drop: NEVER reaches SQLite, NEVER reaches UI, NEVER triggers notifications
        }

        val replyMarkupStr = message.reply_markup?.toString() ?: ""
        val callRegex = Regex("(https?://[^\"]+call\\.html#join=[^\"]+)")
        val match = callRegex.find(replyMarkupStr)
        
        // All incoming messages from polling are from Client B (isFromMe = false)
        var mediaType: String? = null
        var fileId: String? = null
        var fileSize: Long? = null
        var fileName: String? = null
        var fileUniqueId: String? = null
        var mediaMimeType: String? = null
        var mediaDuration: Int? = null
        var mediaWidth: Int? = null
        var mediaHeight: Int? = null
        var mediaWaveform: String? = null

        if (match != null) {
            val joinUrl = match.value + "&isApp=true"
            val callerIdentifier = message.from?.first_name?.takeIf { it.isNotBlank() }
                ?: message.from?.username?.let { "@${it.removePrefix("@")}" }
                ?: if (prefs.isPeerLinkEnabled && prefs.peerLinkPartnerBotUsername.isNotBlank()) "@${prefs.peerLinkPartnerBotUsername.removePrefix("@")}"
                else repository.getProfileSync(chatId)?.title?.takeIf { it.isNotBlank() } ?: "Partner"
            
            val msgTimestamp = message.date * 1000L
            val now = System.currentTimeMillis()
            val isStale = (now - msgTimestamp) > 45000L // 45 seconds
            
            if (isStale) {
                AppLog.log(LogCategory.BOT_ACTIVITY, "Received stale call request (Offline for ${(now - msgTimestamp) / 1000}s). Skipping ringing.")
                text = "Call Missed"
                mediaType = "call_event"
                
                // If not already in call_history, insert as missed
                val existingMsg = repository.getMessageById(message.message_id)
                if (existingMsg == null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val historyNode = com.mobile.superiorchat.data.entity.CallHistoryNode(
                            timestamp = msgTimestamp,
                            durationSeconds = 0L,
                            isMissed = true,
                            isIncoming = true,
                            callStatus = "MISSED",
                            peerJsId = "",
                            domain = "",
                            partnerName = callerIdentifier
                        )
                        com.mobile.superiorchat.core.AppGraph.database.callHistoryDao().insertCall(historyNode)
                    }
                }
            } else {
                com.mobile.superiorchat.core.call.CallManager.receiveIncomingCall(joinUrl, callerIdentifier, message.message_id)
                if (prefs.isPeerLinkEnabled) {
                    text = "Incoming Call"
                    mediaType = "call_event"
                    monitorLocalIncomingCall(chatId, message.message_id, callerIdentifier)
                } else {
                    text = "📞 Incoming Call"
                }
            }
        } else if (prefs.isPeerLinkEnabled && (message.from?.is_bot == true || !com.mobile.superiorchat.utils.Validator.isValidGroupChatId(chatId)) && text.contains("===================") && (text.contains("Time :") || text.contains("*Time* :") || text.contains("<b>Time</b> :")) && (text.contains("New Call Incoming") || text.contains("Call Missed") || text.contains("Call Cancelled") || text.contains("Call Declined") || text.contains("Call Ended") || text.contains("inviting you for call"))) {
            mediaType = "call_event"
            val callStatus = when {
                text.contains("Call Declined") -> "DECLINED"
                text.contains("Call Ended") -> "COMPLETED"
                else -> "MISSED"
            }
            text = when (callStatus) {
                "DECLINED" -> "Call Declined"
                "COMPLETED" -> "Call Ended"
                else -> "Call Missed"
            }

            // If this message is not yet in our database, also log to call_history
            val existingMsg = repository.getMessageById(message.message_id)
            if (existingMsg == null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val callerIdentifier = message.from?.first_name?.takeIf { it.isNotBlank() }
                        ?: message.from?.username?.let { "@${it.removePrefix("@")}" }
                        ?: if (prefs.peerLinkPartnerBotUsername.isNotBlank()) "@${prefs.peerLinkPartnerBotUsername.removePrefix("@")}"
                        else repository.getProfileSync(chatId)?.title?.takeIf { it.isNotBlank() } ?: "Partner"
                    val historyNode = com.mobile.superiorchat.data.entity.CallHistoryNode(
                        timestamp = message.date * 1000L,
                        durationSeconds = 0L,
                        isMissed = callStatus != "COMPLETED",
                        isIncoming = true,
                        callStatus = callStatus,
                        peerJsId = "",
                        domain = "",
                        partnerName = callerIdentifier
                    )
                    com.mobile.superiorchat.core.AppGraph.database.callHistoryDao().insertCall(historyNode)
                }
            }
        }

        if (!message.photo.isNullOrEmpty()) {
            mediaType = "photo"
            val photoObj = message.photo.last().jsonObject
            fileId = photoObj["file_id"]?.jsonPrimitive?.content
            fileUniqueId = photoObj["file_unique_id"]?.jsonPrimitive?.content
            fileSize = photoObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            mediaWidth = photoObj["width"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaHeight = photoObj["height"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaMimeType = "image/jpeg"
        } else if (message.document != null) {
            mediaType = "document"
            val docObj = message.document.jsonObject
            fileId = docObj["file_id"]?.jsonPrimitive?.content
            fileUniqueId = docObj["file_unique_id"]?.jsonPrimitive?.content
            fileSize = docObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = docObj["file_name"]?.jsonPrimitive?.content
            mediaMimeType = docObj["mime_type"]?.jsonPrimitive?.content
        } else if (message.video != null) {
            mediaType = "video"
            val vidObj = message.video.jsonObject
            fileId = vidObj["file_id"]?.jsonPrimitive?.content
            fileUniqueId = vidObj["file_unique_id"]?.jsonPrimitive?.content
            fileSize = vidObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = vidObj["file_name"]?.jsonPrimitive?.content
            mediaDuration = vidObj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaWidth = vidObj["width"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaHeight = vidObj["height"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaMimeType = vidObj["mime_type"]?.jsonPrimitive?.content ?: "video/mp4"
        } else if (message.audio != null) {
            mediaType = "audio"
            val audioObj = message.audio.jsonObject
            fileId = audioObj["file_id"]?.jsonPrimitive?.content
            fileUniqueId = audioObj["file_unique_id"]?.jsonPrimitive?.content
            fileSize = audioObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = audioObj["file_name"]?.jsonPrimitive?.content
            mediaDuration = audioObj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaMimeType = audioObj["mime_type"]?.jsonPrimitive?.content ?: "audio/mpeg"
        } else if (message.voice != null) {
            mediaType = "voice"
            val voiceObj = message.voice.jsonObject
            fileId = voiceObj["file_id"]?.jsonPrimitive?.content
            fileUniqueId = voiceObj["file_unique_id"]?.jsonPrimitive?.content
            fileSize = voiceObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            mediaDuration = voiceObj["duration"]?.jsonPrimitive?.content?.toIntOrNull()
            mediaMimeType = voiceObj["mime_type"]?.jsonPrimitive?.content ?: "audio/ogg"
            mediaWaveform = voiceObj["waveform"]?.jsonPrimitive?.content
        }

        if (fileSize != null && fileSize > 20 * 1024 * 1024) {
            val formattedSize = com.mobile.superiorchat.utils.FileUtils.formatFileSize(fileSize)
            val replyText = """
                *Failed To Upload*
                
                Your file is not delivered because the file size $formattedSize is more than 20MB.
                
                more than 20MB files are not supported.
            """.trimIndent()
            
            val token = prefs.botToken
            if (token.isNotEmpty()) {
                coroutineScope.launch {
                    TelegramApi.sendMessage(
                        token = token,
                        chatId = chatId,
                        text = replyText,
                        parseMode = "Markdown",
                        replyToMessageId = message.message_id
                    )
                }
            }
            AppLog.log(LogCategory.BOT_ACTIVITY, "Rejected incoming file > 20MB: $formattedSize")
            return
        }

        // Check if matching media file ALREADY exists locally in app storage (received or sent folder)
        val existingLocalFile = if (mediaType != null) {
            LocalDirs.findExistingMedia(context, mediaType, fileUniqueId, message.message_id, fileName, fileSize)
        } else null

        val localPath = if (existingLocalFile != null) LocalDirs.toRelativePath(context, existingLocalFile) else null
        val isAlreadyOnDisk = existingLocalFile != null

        val isAutoDownload = AppGraph.prefs.isAutoDownloadMediaEnabled && fileId != null && mediaType != null
        val finalStatus = if (!isAlreadyOnDisk && isAutoDownload) MessageStatus.SENDING else MessageStatus.SENT

        val receiveTimestamp = System.currentTimeMillis()
        
        var parsedText = if (mediaType == "call_event") text else com.mobile.superiorchat.utils.Validator.applyTelegramEntities(text, message.entities ?: message.caption_entities)
        var parsedMediaType = mediaType
        
        val existingChat = repository.getChatSync(chatId)
        var newPinnedMessageId = existingChat?.pinnedMessageId
        
        if (message.pinned_message != null) {
            parsedMediaType = "system_pin"
            parsedText = "${message.from?.first_name ?: "User"} pinned a message"
            newPinnedMessageId = message.pinned_message.message_id
        }

        val isBot = message.from?.is_bot == true || message.from?.username?.endsWith("bot", ignoreCase = true) == true
        val senderUsername = message.from?.username
        val senderName = message.from?.first_name ?: message.from?.last_name ?: "User"
        val senderPhotoPath = existingChat?.photoPath ?: repository.getProfileSync(senderId)?.profilePhotoPath
        val chatType = message.chat.type
        val forwardFromId = message.forward_from?.id?.toString() ?: message.forward_from_chat?.id?.toString()
        val forwardFromName = message.forward_from?.first_name ?: message.forward_from_chat?.title
        val forwardDate = message.forward_date?.let { it * 1000L }
        val rawEntitiesJson = message.entities?.let { entitiesList ->
            try {
                kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(com.mobile.superiorchat.bot.MessageEntity.serializer()),
                    entitiesList
                )
            } catch (e: Exception) { null }
        } ?: message.caption_entities?.let { entitiesList ->
            try {
                kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(com.mobile.superiorchat.bot.MessageEntity.serializer()),
                    entitiesList
                )
            } catch (e: Exception) { null }
        }
        val replyText = message.reply_to_message?.text ?: message.reply_to_message?.caption
        val replyAuthor = message.reply_to_message?.from?.first_name ?: message.reply_to_message?.from?.username

        val conversationEntity = ChatNode(
            chatId = chatId,
            title = message.from?.first_name ?: message.chat.first_name ?: existingChat?.title ?: "Unknown",
            lastMessageText = parsedText,
            lastMessageTimestamp = receiveTimestamp,
            unreadCount = (existingChat?.unreadCount ?: 0) + 1,
            pinnedMessageId = newPinnedMessageId,
            chatType = chatType
        )
        repository.insertOrUpdateConversation(conversationEntity)

        val messageEntity = MessageNode(
            messageId = message.message_id,
            conversationId = chatId,
            senderId = senderId,
            text = parsedText,
            timestamp = receiveTimestamp,
            isFromMe = false,
            isFromBot = isBot,
            senderUsername = senderUsername,
            senderName = senderName,
            senderPhotoPath = senderPhotoPath,
            chatType = chatType,
            mediaType = parsedMediaType,
            mediaUrl = if (fileId != null && fileUniqueId != null) "$fileId|$fileUniqueId" else fileId,
            mediaLocalPath = localPath,
            status = finalStatus,
            mediaFileName = fileName,
            mediaFileSize = fileSize,
            mediaMimeType = mediaMimeType,
            mediaDuration = mediaDuration,
            mediaWidth = mediaWidth,
            mediaHeight = mediaHeight,
            mediaWaveform = mediaWaveform,
            entities = rawEntitiesJson,
            replyToMessageId = message.reply_to_message?.message_id,
            replyToText = replyText,
            replyToAuthor = replyAuthor,
            forwardFromId = forwardFromId,
            forwardFromName = forwardFromName,
            forwardDate = forwardDate
        )

        repository.insertMessage(messageEntity)

        // Trigger profile sync for sender if missing or out of date
        if (senderId.isNotEmpty()) {
            val senderNameSync = message.from?.first_name
            val senderUsernameSync = message.from?.username
            coroutineScope.launch(Dispatchers.IO) {
                if (isNetworkAvailable) {
                    val token = prefs.botToken
                    val cached = repository.getProfileSync(senderId)
                    if (cached == null || (senderName != null && cached.title != senderName) || (senderUsername != null && cached.username != senderUsername)) {
                        MediaSync.syncSenderProfile(context, token, senderId, senderName, senderUsername, silent = true)
                    }
                }
            }
        }

        if (!isAlreadyOnDisk && isAutoDownload && fileId != null && mediaType != null) {
            MediaSync.enqueueDownload(context, messageEntity.messageId, fileId, mediaType)
        }

        AppLog.log(LogCategory.BOT_ACTIVITY, "Received message: ${text.take(50)}")

        // Route for notification
        if (message.chat.id.toString() == prefs.activeChatId && !isAlreadyOnDisk) {
            // Don't show push notifications for incoming peerlink calls, the ringer handles it!
            if (!(prefs.isPeerLinkEnabled && mediaType == "call_event")) {
                notifier.routeUpdate(update)
            }
        }
    }

    private fun flushQueuedMessages() {
        coroutineScope.launch(Dispatchers.IO) {
            if (!isNetworkAvailable) return@launch
            val token = prefs.botToken
            if (token.isEmpty()) return@launch
            try {
                val queuedMessages = repository.getQueuedMessages()
                for (msg in queuedMessages) {
                    if (SendRateLimiter.isBlocked()) {
                        AppLog.log(LogCategory.BOT_ACTIVITY, "[QUEUE] Rate limit or cooldown active, holding flush")
                        break
                    }
                    if (msg.mediaType == null) {
                        repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                        repository.sendTextMessage(token, msg.conversationId, msg.text ?: "", msg.messageId)
                    } else {
                        repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                        MediaSync.enqueueUpload(context, msg.messageId, msg.mediaLocalPath ?: "", msg.mediaType)
                    }
                    delay(500L)
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to flush queued messages: ${e.message}")
            }
        }
    }

    fun forceSyncProfile() {
        syncTargetProfile()
    }

    private fun syncTargetProfile() {
        coroutineScope.launch(Dispatchers.IO) {
            if (!isNetworkAvailable) return@launch
            val token = prefs.botToken
            val chatId = prefs.activeChatId
            if (token.isEmpty() || chatId.isEmpty()) return@launch
            MediaSync.syncTargetProfile(context, token, chatId)
        }
    }

    private fun monitorLocalIncomingCall(chatId: String, telegramMsgId: Long, callerIdentifier: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Wait for the call to transition to a finished state (ENDING or IDLE)
                com.mobile.superiorchat.core.call.CallManager.callState.first { it == com.mobile.superiorchat.core.call.CallState.ENDING || it == com.mobile.superiorchat.core.call.CallState.IDLE }
                
                val duration = if (com.mobile.superiorchat.core.call.CallManager.callDuration.value > 0) com.mobile.superiorchat.core.call.CallManager.callDuration.value else com.mobile.superiorchat.core.call.CallManager.lastCompletedDuration
                val isMissed = duration == 0L
                val lastError = com.mobile.superiorchat.core.call.CallManager.lastCallFailedDueToError.value
                val domain = com.mobile.superiorchat.core.call.CallManager.currentBaseUrl ?: ""
                val peerJsId = com.mobile.superiorchat.core.call.CallManager.currentRoomId ?: ""
                
                val localText = if (isMissed) {
                    when (lastError) {
                        com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "Network Error"
                        com.mobile.superiorchat.core.call.CallError.DECLINED -> "Call Declined"
                        else -> "Call Missed"
                    }
                } else {
                    "Call Ended - ${com.mobile.superiorchat.core.call.CallManager.formatDurationText(duration)}"
                }
                
                repository.updateMessageText(telegramMsgId, localText)
                
                val status = when {
                    duration > 0L -> "COMPLETED"
                    lastError == com.mobile.superiorchat.core.call.CallError.NETWORK_ERROR -> "FAILED_NETWORK"
                    lastError == com.mobile.superiorchat.core.call.CallError.DECLINED -> "DECLINED"
                    else -> "MISSED"
                }
                
                val node = com.mobile.superiorchat.data.entity.CallHistoryNode(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration,
                    isMissed = isMissed,
                    isIncoming = true,
                    callStatus = status,
                    peerJsId = peerJsId,
                    domain = domain,
                    partnerName = callerIdentifier
                )
                AppGraph.database.callHistoryDao().insertCall(node)
                
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to monitor incoming call termination: ${e.message}")
            }
        }
    }
}
