package com.mobile.superiorutils.bot

import android.content.Context
import com.mobile.superiorutils.core.NetState
import com.mobile.superiorutils.core.AppGraph

import com.mobile.superiorutils.data.entity.ChatNode
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.mobile.superiorutils.media.MediaSync

class BotSync(private val context: Context) {

    private val prefs = AppGraph.prefs
    private val repository = AppGraph.chatRepository
    private val messageRouter = Notifier(context, CoroutineScope(Dispatchers.IO))

    private var pollingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastUpdateId: Long
        get() = prefs.lastUpdateId
        set(value) {
            prefs.lastUpdateId = value
        }
    private var networkCollectorJob: Job? = null
    private val networkWakeChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    @Volatile private var isNetworkAvailable = false

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        registerNetworkCallback()
        launchPollingLoop()
        flushQueuedMessages()
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
                        AppLog.log(LogCategory.SYSTEM, "Network available. Waking up polling loop and flushing queue.")
                        networkWakeChannel.trySend(Unit)
                        flushQueuedMessages()
                    } else {
                        AppLog.log(LogCategory.SYSTEM, "Network lost. Polling will pause after current request times out.")
                        AppLog.setTelegramApiReachable(false)
                    }
                }
            }
            launch {
                AppLog.isTelegramApiReachable.collect { isReachable ->
                    // If the user manually retries from the UI and succeeds, wake up the polling loop!
                    if (isReachable && isNetworkAvailable) {
                        AppLog.log(LogCategory.SYSTEM, "API reachable via UI retry. Waking up polling loop.")
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
                                    for (update in updateResponse.result) {
                                        try {
                                            lastUpdateId = update.update_id
                                            handleUpdate(update)
                                        } catch (e: Exception) {
                                            AppLog.log(LogCategory.SYSTEM, "Failed to handle update ${update.update_id}: ${e.message}", LogLevel.ERROR)
                                        }
                                    }
                                }
                            }
                        } else {
                            if (response.code == 409) {
                                consecutiveFailures += 2 // Give other instances time to die
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
                    kotlinx.coroutines.withTimeoutOrNull(backoffMs) {
                        networkWakeChannel.receive()
                    }
                }
            }
        }
    }

    private suspend fun handleUpdate(update: Update) {
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
            val formattedSize = com.mobile.superiorutils.utils.FileUtils.formatFileSize(fileSize)
            val replyText = """
                *Failed*
                
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

        val conversationEntity = ChatNode(
            chatId = chatId,
            title = message.from?.first_name ?: message.chat.first_name ?: "Unknown",
            lastMessageText = text,
            lastMessageTimestamp = receiveTimestamp,
            unreadCount = 1
        )
        repository.insertOrUpdateConversation(conversationEntity)

        val messageEntity = MessageNode(
            messageId = message.message_id,
            conversationId = chatId,
            senderId = senderId,
            text = text,
            timestamp = receiveTimestamp,
            isFromMe = false,
            mediaType = mediaType,
            mediaUrl = fileId, // Store file ID inside mediaUrl for potential redownload retries
            status = finalStatus,
            mediaFileName = fileName,
            mediaFileSize = fileSize
        )

        repository.insertMessage(messageEntity)

        if (isAutoDownload) {
            MediaSync.enqueueDownload(context, messageEntity.messageId, fileId!!, mediaType!!)
        }

        AppLog.log(LogCategory.BOT_ACTIVITY, "Received message: ${text.take(50)}")

        // Route for notification
        messageRouter.routeUpdate(update)
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
                        val sentId = TelegramApi.sendMessage(token, msg.conversationId, msg.text ?: "")
                        if (sentId != null) {
                            repository.updateMessageStatus(msg.messageId, MessageStatus.SENT)
                        } else {
                            repository.updateMessageStatus(msg.messageId, MessageStatus.FAILED)
                        }
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
}
