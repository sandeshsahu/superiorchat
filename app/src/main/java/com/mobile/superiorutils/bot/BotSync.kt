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
    private var lastUpdateId = 0L
    private var networkCollectorJob: Job? = null
    private val networkWakeChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    @Volatile private var isNetworkAvailable = false

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        registerNetworkCallback()
        launchPollingLoop()
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

                try {
                    val token = prefs.botToken
                    val ownerId = prefs.ownerUserId

                    if (token.isEmpty()) {
                        AppLog.log(LogCategory.SYSTEM, "Bot token is empty. Pausing polling.")
                        delay(5000)
                        continue
                    }

                    val response = TelegramApi.getUpdatesRaw(token, lastUpdateId + 1, 60)
                    if (response.isSuccessful) {
                        consecutiveFailures = 0
                        AppLog.setTelegramApiReachable(true)

                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val updateResponse = TelegramApi.json.decodeFromString<UpdateResponse>(body)
                            if (updateResponse.ok) {
                                for (update in updateResponse.result) {
                                    lastUpdateId = update.update_id
                                    handleUpdate(update, ownerId)
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
                    response.close()
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
                    333L
                }
                
                // Wait for backoff, or wake up instantly if network becomes available
                kotlinx.coroutines.withTimeoutOrNull(backoffMs) {
                    networkWakeChannel.receive()
                }
            }
        }
    }

    private suspend fun handleUpdate(update: Update, ownerId: String) {
        val message = update.message ?: return

        // Intruder filtering: only accept messages from owner or target chat
        val senderId = message.from?.id?.toString() ?: ""
        if (ownerId.isNotEmpty() && senderId != ownerId && message.chat.id.toString() != ownerId) {
            AppLog.log(LogCategory.BOT_ACTIVITY, "Intruder detected! Ignored message from $senderId", LogLevel.WARN)
            return
        }

        val chatId = message.chat.id.toString()
        val text = message.text ?: ""

        // All incoming messages from polling are from Client B (isFromMe = false)
        var mediaType: String? = null
        var fileId: String? = null

        if (!message.photo.isNullOrEmpty()) {
            mediaType = "photo"
            fileId = message.photo.last().jsonObject["file_id"]?.jsonPrimitive?.content
        } else if (message.document != null) {
            mediaType = "document"
            fileId = message.document.jsonObject["file_id"]?.jsonPrimitive?.content
        } else if (message.video != null) {
            mediaType = "video"
            fileId = message.video.jsonObject["file_id"]?.jsonPrimitive?.content
        } else if (message.audio != null) {
            mediaType = "audio"
            fileId = message.audio.jsonObject["file_id"]?.jsonPrimitive?.content
        } else if (message.voice != null) {
            mediaType = "voice"
            fileId = message.voice.jsonObject["file_id"]?.jsonPrimitive?.content
        }

        val messageEntity = MessageNode(
            messageId = message.message_id,
            conversationId = chatId,
            senderId = senderId,
            text = text,
            timestamp = message.date * 1000L,
            isFromMe = false,
            mediaType = mediaType,
            mediaUrl = fileId, // Store file ID inside mediaUrl for potential redownload retries
            status = if (fileId != null) MessageStatus.SENDING else MessageStatus.SENT
        )

        repository.insertMessage(messageEntity)

        if (fileId != null && mediaType != null) {
            MediaSync.enqueueDownload(context, message.message_id, fileId, mediaType)
            AppLog.log(LogCategory.BOT_ACTIVITY, "Enqueued media download for fileId: $fileId")
        }

        val conversationEntity = ChatNode(
            chatId = chatId,
            title = message.from?.first_name ?: message.chat.first_name ?: "Unknown",
            lastMessageText = text,
            lastMessageTimestamp = message.date * 1000L,
            unreadCount = 1
        )
        repository.insertOrUpdateConversation(conversationEntity)

        AppLog.log(LogCategory.BOT_ACTIVITY, "Received message: ${text.take(50)}")

        // Route for notification
        messageRouter.routeUpdate(update)
    }

    private fun flushQueuedMessages() {
        coroutineScope.launch(Dispatchers.IO) {
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
