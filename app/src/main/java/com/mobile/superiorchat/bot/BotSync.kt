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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.mobile.superiorchat.media.MediaSync
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
                    notifier.setNetworkState(isNetworkAvailable, AppLog.isTelegramApiReachable.value)
                    if (isOnline) {
                        if (StatusFlow.syncState.value == SyncState.OFFLINE) {
                            StatusFlow.reportStatus(SyncState.SUCCESS, "Online")
                        }
                        AppLog.log(LogCategory.SYSTEM, "Network available. Waking up polling loop and flushing queue.")
                        networkWakeChannel.trySend(Unit)
                        flushQueuedMessages()
                    } else {
                        StatusFlow.reportStatus(SyncState.OFFLINE, "Connection offline")
                        AppLog.log(LogCategory.SYSTEM, "Network lost. Polling will pause after current request times out.")
                        AppLog.setTelegramApiReachable(false)
                        showSyncFeedback = true
                    }
                }
            }
            launch {
                AppLog.isTelegramApiReachable.collect { isReachable ->
                    notifier.setNetworkState(isNetworkAvailable, isReachable)
                    if (!isReachable && isNetworkAvailable) {
                        StatusFlow.reportStatus(SyncState.OFFLINE, "Telegram API Unreachable")
                        showSyncFeedback = true
                    } else if (isReachable && isNetworkAvailable) {
                        if (StatusFlow.syncState.value == SyncState.OFFLINE) {
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
        if (update.edited_message != null) {
            val editedMsg = update.edited_message
            if (editedMsg.text != null) {
                repository.updateMessageText(editedMsg.message_id, editedMsg.text)
                AppLog.log(LogCategory.BOT_ACTIVITY, "Updated edited message: ${editedMsg.text.take(50)}")
            }
            return
        }

        val message = update.message ?: return

        // Intruder filtering: only accept messages from the target chat
        val senderId = message.from?.id?.toString() ?: ""
        val targetChatId = AppGraph.prefs.chatId
        if (targetChatId.isNotEmpty() && message.chat.id.toString() != targetChatId) {
            AppLog.log(LogCategory.BOT_ACTIVITY, "Intruder detected! Ignored message from chat ${message.chat.id}", LogLevel.WARN)
            return
        }

        val chatId = message.chat.id.toString()
        val text = message.text ?: ""

        // All incoming messages from polling are from Client B (isFromMe = false)
        var mediaType: String? = null
        var fileId: String? = null
        var fileSize: Long? = null
        var fileName: String? = null

        if (!message.photo.isNullOrEmpty()) {
            mediaType = "photo"
            val photoObj = message.photo.last().jsonObject
            fileId = photoObj["file_id"]?.jsonPrimitive?.content
            fileSize = photoObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
        } else if (message.document != null) {
            mediaType = "document"
            val docObj = message.document.jsonObject
            fileId = docObj["file_id"]?.jsonPrimitive?.content
            fileSize = docObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = docObj["file_name"]?.jsonPrimitive?.content
        } else if (message.video != null) {
            mediaType = "video"
            val vidObj = message.video.jsonObject
            fileId = vidObj["file_id"]?.jsonPrimitive?.content
            fileSize = vidObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = vidObj["file_name"]?.jsonPrimitive?.content
        } else if (message.audio != null) {
            mediaType = "audio"
            val audioObj = message.audio.jsonObject
            fileId = audioObj["file_id"]?.jsonPrimitive?.content
            fileSize = audioObj["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
            fileName = audioObj["file_name"]?.jsonPrimitive?.content
        } else if (message.voice != null) {
            mediaType = "voice"
            fileId = message.voice.jsonObject["file_id"]?.jsonPrimitive?.content
            fileSize = message.voice.jsonObject["file_size"]?.jsonPrimitive?.content?.toLongOrNull()
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

        val isAutoDownload = AppGraph.prefs.isAutoDownloadMediaEnabled && fileId != null && mediaType != null
        val finalStatus = if (isAutoDownload) MessageStatus.SENDING else MessageStatus.SENT

        val receiveTimestamp = System.currentTimeMillis()
        
        var parsedText = text
        var parsedMediaType = mediaType
        
        val existingChat = repository.getChatSync(chatId)
        var newPinnedMessageId = existingChat?.pinnedMessageId
        
        if (message.pinned_message != null) {
            parsedMediaType = "system_pin"
            parsedText = "${message.from?.first_name ?: "User"} pinned a message"
            newPinnedMessageId = message.pinned_message.message_id
        }

        val conversationEntity = ChatNode(
            chatId = chatId,
            title = message.from?.first_name ?: message.chat.first_name ?: existingChat?.title ?: "Unknown",
            lastMessageText = parsedText,
            lastMessageTimestamp = receiveTimestamp,
            unreadCount = (existingChat?.unreadCount ?: 0) + 1,
            pinnedMessageId = newPinnedMessageId
        )
        repository.insertOrUpdateConversation(conversationEntity)

        val messageEntity = MessageNode(
            messageId = message.message_id,
            conversationId = chatId,
            senderId = senderId,
            text = parsedText,
            timestamp = receiveTimestamp,
            isFromMe = false,
            mediaType = parsedMediaType,
            mediaUrl = fileId, // Store file ID inside mediaUrl for potential redownload retries
            status = finalStatus,
            mediaFileName = fileName,
            mediaFileSize = fileSize,
            replyToMessageId = message.reply_to_message?.message_id
        )

        repository.insertMessage(messageEntity)

        if (isAutoDownload) {
            MediaSync.enqueueDownload(context, messageEntity.messageId, fileId!!, mediaType!!)
        }

        AppLog.log(LogCategory.BOT_ACTIVITY, "Received message: ${text.take(50)}")

        // Route for notification
        notifier.routeUpdate(update)
    }

    private fun flushQueuedMessages() {
        coroutineScope.launch(Dispatchers.IO) {
            if (!isNetworkAvailable) return@launch
            val token = prefs.botToken
            if (token.isEmpty()) return@launch
            try {
                val queuedMessages = repository.getQueuedMessages()
                for (msg in queuedMessages) {
                    if (msg.mediaType == null) {
                        repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                        repository.sendTextMessage(token, msg.conversationId, msg.text ?: "", msg.messageId)
                    } else {
                        repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                        MediaSync.enqueueUpload(context, msg.messageId, msg.mediaLocalPath ?: "", msg.mediaType)
                    }
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
            val chatId = prefs.chatId
            if (token.isEmpty() || chatId.isEmpty()) return@launch
            MediaSync.syncTargetProfile(context, token, chatId)
        }
    }
}
