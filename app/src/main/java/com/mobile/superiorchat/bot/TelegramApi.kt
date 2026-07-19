package com.mobile.superiorchat.bot

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit
import okio.BufferedSink
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RateLimitException(val retryAfterSeconds: Int, message: String) : Exception(message)
class TelegramApiException(message: String) : Exception(message)
@Serializable
private data class LinkPreviewOptions(
    @SerialName("is_disabled") val isDisabled: Boolean
)

@Serializable
private data class SendMessageRequest(
    @SerialName("chat_id") val chatId: String,
    val text: String,
    @SerialName("parse_mode") val parseMode: String? = null,
    @SerialName("reply_markup") val replyMarkup: JsonElement? = null,
    @SerialName("link_preview_options") val linkPreviewOptions: LinkPreviewOptions? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null
)

/**
 * Centralized Telegram Bot API client.
 *
 * Single [OkHttpClient] instance and token sanitization logic shared across
 * all components (BotService, SnapshotEngine, BotCommands).
 */
object TelegramApi {

    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(85, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val json: Json = Json { ignoreUnknownKeys = true }



    /** Strip leading non-alphanumeric chars (BOM, invisible Unicode, etc.) from stored tokens. */
    fun sanitizeToken(rawToken: String): String =
        rawToken.trim().replace(Regex("^[^a-zA-Z0-9]+"), "")

    private fun apiUrl(token: String, method: String): String =
        "https://api.telegram.org/bot${sanitizeToken(token)}/$method"

    // ═══════════════════════════════════════════════════════════
    //  POLLING & IDENTITY
    // ═══════════════════════════════════════════════════════════

    /**
     * Execute a long-poll getUpdates call.
     * Returns the raw OkHttp [Response] so the caller retains full control
     * over reachability tracking and error handling.
     */
    fun getUpdatesRaw(token: String, offset: Long, timeout: Int = 30): Response {
        val allowedUpdates = "%5B%22message%22%2C%22edited_message%22%2C%22message_reaction%22%5D"
        val url = apiUrl(token, "getUpdates") + "?offset=$offset&timeout=$timeout&allowed_updates=$allowedUpdates"
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute()
    }

    /** Fetch bot identity via /getMe. Returns null on any failure. */
    fun getMe(token: String): GetMeResponse? {
        return try {
            val request = Request.Builder().url(apiUrl(token, "getMe")).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { json.decodeFromString<GetMeResponse>(it) }
            } else null
        } catch (e: Exception) { null }
    }

    fun getFile(token: String, fileId: String): FileResponse? {
        return try {
            val request = Request.Builder().url(apiUrl(token, "getFile") + "?file_id=$fileId").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { json.decodeFromString<FileResponse>(it) }
                } else null
            }
        } catch (e: Exception) { null }
    }

    /** Helper to construct file download URL */
    fun getFileDownloadUrl(token: String, filePath: String): String {
        return "https://api.telegram.org/file/bot${sanitizeToken(token)}/$filePath"
    }

    fun getChat(token: String, chatId: String): ChatResponse? {
        return try {
            val request = Request.Builder().url(apiUrl(token, "getChat") + "?chat_id=$chatId").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { json.decodeFromString<ChatResponse>(it) }
                } else null
            }
        } catch (e: Exception) { null }
    }

    fun downloadFileToLocal(url: String, destFile: File): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        body.byteStream().use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } else false
                } else false
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "downloadFileToLocal error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SENDING MESSAGES
    // ═══════════════════════════════════════════════════════════

    /** Send a text message. Returns the message ID on success, or null on failure. */
    fun sendMessage(
        token: String,
        chatId: String,
        text: String,
        parseMode: String? = "Markdown",
        replyMarkup: String? = null,
        replyToMessageId: Long? = null
    ): Long? {
        val delayMs = SendRateLimiter.acquire()
        if (delayMs > 0L) {
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                // Ignore
            }
        }
        return try {
            val markupJson = replyMarkup?.let { json.parseToJsonElement(it) }
            val req = SendMessageRequest(chatId, text, parseMode, markupJson, LinkPreviewOptions(isDisabled = true), replyToMessageId)
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "sendMessage"))
                .post(body)
                .build()

            var messageId: Long? = null
            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "sendMessage failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] " + text.take(200))
                    val respBody = response.body?.string()
                    if (respBody != null) {
                        try {
                            val jsonObject = json.parseToJsonElement(respBody).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendMessage response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
            }
            messageId
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendMessage error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }



    /** Send a photo file. Returns the message ID on success, null on failure. */
    suspend fun sendPhoto(
        token: String,
        chatId: String,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long? {
        return try {
            val photoBody = if (onProgress != null) {
                ProgressRequestBody(file, "image/jpeg".toMediaType(), onProgress)
            } else {
                file.asRequestBody("image/jpeg".toMediaType())
            }
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("photo", file.name, photoBody)
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }
            if (replyToMessageId != null) {
                builder.addFormDataPart("reply_to_message_id", replyToMessageId.toString())
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendPhoto"))
                .post(builder.build())
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                var messageId: Long? = null
                if (!success) {
                    AppLog.log(LogCategory.NETWORK, "sendPhoto failed: ${response.code}", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Photo: ${file.name}" + (if (caption != null) " - ${caption.take(100)}" else ""))
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendPhoto response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
                messageId
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendPhoto error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }

    /** Send a voice audio file. Returns the message ID on success, null on failure. */
    suspend fun sendVoice(
        token: String,
        chatId: String,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long? {
        return try {
            val voiceBody = if (onProgress != null) {
                ProgressRequestBody(file, "audio/mp4".toMediaType(), onProgress)
            } else {
                file.asRequestBody("audio/mp4".toMediaType())
            }
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("voice", file.name, voiceBody)
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }
            if (replyToMessageId != null) {
                builder.addFormDataPart("reply_to_message_id", replyToMessageId.toString())
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendVoice"))
                .post(builder.build())
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                var messageId: Long? = null
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "sendVoice failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Voice: ${file.name}")
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendVoice response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
                messageId
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendVoice error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }

    /** Send an audio file (e.g. mp3). Returns the message ID on success, null on failure. */
    suspend fun sendAudio(
        token: String,
        chatId: String,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long? {
        return try {
            val audioBody = if (onProgress != null) {
                ProgressRequestBody(file, "audio/mpeg".toMediaType(), onProgress)
            } else {
                file.asRequestBody("audio/mpeg".toMediaType())
            }
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("audio", file.name, audioBody)
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }
            if (replyToMessageId != null) {
                builder.addFormDataPart("reply_to_message_id", replyToMessageId.toString())
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendAudio"))
                .post(builder.build())
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                var messageId: Long? = null
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "sendAudio failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Audio: ${file.name}")
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendAudio response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
                messageId
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendAudio error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }

    /** Send a video file. Returns the message ID on success, null on failure. */
    suspend fun sendVideo(
        token: String,
        chatId: String,
        file: File,
        caption: String? = null,
        replyToMessageId: Long? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long? {
        return try {
            val videoBody = if (onProgress != null) {
                ProgressRequestBody(file, "video/mp4".toMediaType(), onProgress)
            } else {
                file.asRequestBody("video/mp4".toMediaType())
            }
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("video", file.name, videoBody)
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }
            if (replyToMessageId != null) {
                builder.addFormDataPart("reply_to_message_id", replyToMessageId.toString())
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendVideo"))
                .post(builder.build())
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                var messageId: Long? = null
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "sendVideo failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Video: ${file.name}")
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendVideo response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
                messageId
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendVideo error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }

    /** Send a document file. Returns the message ID on success, null on failure. */
    suspend fun sendDocument(
        token: String,
        chatId: String,
        file: File,
        caption: String,
        parseMode: String = "Markdown",
        displayName: String? = null,
        replyToMessageId: Long? = null,
        onProgress: ((Long, Long) -> Unit)? = null
    ): Long? {
        return try {
            // Use displayName if provided, otherwise fall back to file.name
            val uploadName = displayName ?: file.name
            val docBody = if (onProgress != null) {
                ProgressRequestBody(file, "application/octet-stream".toMediaType(), onProgress)
            } else {
                file.asRequestBody("application/octet-stream".toMediaType())
            }
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart("parse_mode", parseMode)
                .addFormDataPart("document", uploadName, docBody)
            
            if (replyToMessageId != null) {
                requestBodyBuilder.addFormDataPart("reply_to_message_id", replyToMessageId.toString())
            }
                
            val requestBody = requestBodyBuilder.build()

            val request = Request.Builder()
                .url(apiUrl(token, "sendDocument"))
                .post(requestBody)
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                var messageId: Long? = null
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "sendDocument failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Document: ${file.name} - ${caption.take(100)}")
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
                            val result = jsonObject["result"]?.jsonObject
                            messageId = result?.get("message_id")?.jsonPrimitive?.long
                        } catch (e: Exception) {
                            AppLog.log(LogCategory.NETWORK, "Failed to parse sendDocument response: ${e.message}", com.mobile.superiorchat.utils.LogLevel.WARN)
                        }
                    }
                }
                messageId
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendDocument error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            null
        }
    }



    // ═══════════════════════════════════════════════════════════
    //  PINNED MESSAGES
    // ═══════════════════════════════════════════════════════════
    suspend fun pinChatMessage(
        token: String,
        chatId: String,
        messageId: Long
    ): Boolean {
        return try {
            @Serializable
            data class PinMessageRequest(
                @SerialName("chat_id") val chatId: String,
                @SerialName("message_id") val messageId: Long,
                @SerialName("disable_notification") val disableNotification: Boolean = false
            )
            
            val req = PinMessageRequest(chatId, messageId, false)
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "pinChatMessage"))
                .post(body)
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "pinChatMessage failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                }
                success
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "pinChatMessage error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }

    suspend fun unpinChatMessage(
        token: String,
        chatId: String,
        messageId: Long
    ): Boolean {
        return try {
            @Serializable
            data class UnpinMessageRequest(
                @SerialName("chat_id") val chatId: String,
                @SerialName("message_id") val messageId: Long
            )
            
            val req = UnpinMessageRequest(chatId, messageId)
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "unpinChatMessage"))
                .post(body)
                .build()

            client.executeCancellable(request).use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "unpinChatMessage failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                }
                success
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "unpinChatMessage error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }


    // ═══════════════════════════════════════════════════════════
    //  MESSAGE EDITING & DELETION
    // ═══════════════════════════════════════════════════════════

    fun editMessageText(
        token: String,
        chatId: String,
        messageId: Long,
        text: String,
        parseMode: String? = "Markdown"
    ): Boolean {
        return try {
            @Serializable
            data class EditMessageRequest(
                @SerialName("chat_id") val chatId: String,
                @SerialName("message_id") val messageId: Long,
                val text: String,
                @SerialName("parse_mode") val parseMode: String? = null
            )
            
            val req = EditMessageRequest(chatId, messageId, text, parseMode)
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "editMessageText"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "editMessageText failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[EDITMSG] " + text.take(100))
                }
                success
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "editMessageText error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }

    fun deleteMessage(
        token: String,
        chatId: String,
        messageId: Long
    ): Boolean {
        return try {
            @Serializable
            data class DeleteMessageRequest(
                @SerialName("chat_id") val chatId: String,
                @SerialName("message_id") val messageId: Long
            )
            
            val req = DeleteMessageRequest(chatId, messageId)
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "deleteMessage"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "deleteMessage failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[DELMSG] ID $messageId")
                }
                success
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "deleteMessage error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }

    /**
     * Send a reaction emoji to a message via setMessageReaction.
     * Pass an empty [emoji] to remove all reactions.
     * Returns true on success.
     */
    fun setMessageReaction(
        token: String,
        chatId: String,
        messageId: Long,
        emoji: String
    ): Boolean {
        return try {
            // Build JSON manually — the reactions field is an array of ReactionType objects
            val reactionsArray = if (emoji.isBlank()) {
                "[]"
            } else {
                """[{"type":"emoji","emoji":"$emoji"}]"""
            }
            val jsonBody = """{"chat_id":"$chatId","message_id":$messageId,"reaction":$reactionsArray}"""
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "setMessageReaction"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                if (!success) {
                    val errorBody = response.body?.string()
                    AppLog.log(LogCategory.NETWORK, "setMessageReaction failed: ${response.code} - $errorBody", com.mobile.superiorchat.utils.LogLevel.ERROR)
                } else {
                    AppLog.log(LogCategory.BOT_ACTIVITY, "[REACT] $emoji on $messageId")
                }
                success
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "setMessageReaction error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false
        }
    }

    /** Verifies BOTH local internet capabilities and Telegram API reachability. */
    fun isApiReachable(context: Context, token: String): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.activeNetwork == null) return false
        return getMe(token) != null
    }
    // ═══════════════════════════════════════════════════════════
    //  BOT IDENTITY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    suspend fun getMyDescription(token: String): String {
        return try {
            val request = Request.Builder().url(apiUrl(token, "getMyDescription")).build()
            val response = client.executeCancellable(request)
            if (response.isSuccessful) {
                response.body?.string()?.let { 
                    json.decodeFromString<BotDescriptionResponse>(it).result?.description ?: ""
                } ?: ""
            } else ""
        } catch (e: Exception) { "" }
    }

    suspend fun getMyShortDescription(token: String): String {
        return try {
            val request = Request.Builder().url(apiUrl(token, "getMyShortDescription")).build()
            val response = client.executeCancellable(request)
            if (response.isSuccessful) {
                response.body?.string()?.let { 
                    json.decodeFromString<BotShortDescriptionResponse>(it).result?.shortDescription ?: ""
                } ?: ""
            } else ""
        } catch (e: Exception) { "" }
    }

    suspend fun getMyProfilePhotoUrl(token: String): String? {
        val botId = sanitizeToken(token).split(":")[0]
        return try {
            val request = Request.Builder()
                .url(apiUrl(token, "getUserProfilePhotos") + "?user_id=$botId&limit=1")
                .build()
            val response = client.executeCancellable(request)
            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    val result = json.decodeFromString<UserProfilePhotosResponse>(body).result
                    val photos = result?.photos
                    if (!photos.isNullOrEmpty() && photos.first().isNotEmpty()) {
                        val largestPhoto = photos.first().maxByOrNull { it.width * it.height }
                        if (largestPhoto != null) {
                            val fileInfo = getFile(token, largestPhoto.fileId)
                            if (fileInfo?.result?.file_path != null) {
                                getFileDownloadUrl(token, fileInfo.result.file_path)
                            } else null
                        } else null
                    } else null
                }
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun setMyName(token: String, name: String): Boolean {
        val jsonBody = org.json.JSONObject().apply { put("name", name) }.toString()
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(apiUrl(token, "setMyName")).post(body).build()
        val response = client.executeCancellable(request)
        if (!response.isSuccessful) {
            parseErrorAndThrow(response, "setMyName")
        }
        return response.isSuccessful
    }

    suspend fun setMyDescription(token: String, description: String): Boolean {
        val jsonBody = org.json.JSONObject().apply { put("description", description) }.toString()
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(apiUrl(token, "setMyDescription")).post(body).build()
        val response = client.executeCancellable(request)
        if (!response.isSuccessful) {
            parseErrorAndThrow(response, "setMyDescription")
        }
        return response.isSuccessful
    }

    suspend fun setMyShortDescription(token: String, shortDescription: String): Boolean {
        val jsonBody = org.json.JSONObject().apply { put("short_description", shortDescription) }.toString()
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(apiUrl(token, "setMyShortDescription")).post(body).build()
        val response = client.executeCancellable(request)
        if (!response.isSuccessful) {
            parseErrorAndThrow(response, "setMyShortDescription")
        }
        return response.isSuccessful
    }

    suspend fun setMyProfilePhoto(token: String, photoFile: java.io.File): Boolean {
        return try {
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart(
                    "photo",
                    """{"type":"static","photo":"attach://profile_pic"}"""
                )
                .addFormDataPart(
                    "profile_pic",
                    photoFile.name,
                    photoFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()
            val request = Request.Builder()
                .url(apiUrl(token, "setMyProfilePhoto"))
                .post(requestBody)
                .build()
            val response = client.executeCancellable(request)
            val bodyStr = response.body?.string()
            if (!response.isSuccessful) {
                AppLog.log(LogCategory.NETWORK, "setMyProfilePhoto failed: ${response.code} $bodyStr", com.mobile.superiorchat.utils.LogLevel.ERROR)
            }
            response.isSuccessful
        } catch (e: Exception) { 
            AppLog.log(LogCategory.NETWORK, "setMyProfilePhoto error: ${e.message}", com.mobile.superiorchat.utils.LogLevel.ERROR)
            false 
        }
    }

    private fun parseErrorAndThrow(response: Response, methodName: String) {
        val bodyStr = response.body?.string() ?: ""
        AppLog.log(LogCategory.NETWORK, "$methodName failed: ${response.code} $bodyStr", com.mobile.superiorchat.utils.LogLevel.ERROR)
        try {
            val json = org.json.JSONObject(bodyStr)
            val desc = json.optString("description", "Unknown Telegram Error")
            if (response.code == 429) {
                val params = json.optJSONObject("parameters")
                val retryAfter = params?.optInt("retry_after", 0) ?: 0
                if (retryAfter > 0) throw RateLimitException(retryAfter, desc)
            }
            throw TelegramApiException("API Error ${response.code}: $desc")
        } catch (e: org.json.JSONException) {
            throw TelegramApiException("HTTP ${response.code}: $bodyStr")
        }
    }
}

object SendRateLimiter {
    private const val MAX_TOKENS = 3
    private const val REFILL_RATE_PER_SEC = 3.0
    
    private var tokens = MAX_TOKENS.toDouble()
    private var lastRefillTime = System.currentTimeMillis()

    @Synchronized
    fun acquire(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        lastRefillTime = now
        
        tokens = minOf(MAX_TOKENS.toDouble(), tokens + elapsed * (REFILL_RATE_PER_SEC / 1000.0))
        
        if (tokens >= 1.0) {
            tokens -= 1.0
            return 0L
        }
        
        val missingToken = 1.0 - tokens
        val delayMs = (missingToken * 1000.0 / REFILL_RATE_PER_SEC).toLong()
        tokens = 0.0
        lastRefillTime = now + delayMs
        return delayMs
    }
}

class ProgressRequestBody(
    private val file: File,
    private val contentType: okhttp3.MediaType?,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : okhttp3.RequestBody() {
    override fun contentType() = contentType
    override fun contentLength() = file.length()
    override fun writeTo(sink: okio.BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(8192)
        var uploaded = 0L
        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, fileLength)
            }
        }
    }
}

suspend fun okhttp3.OkHttpClient.executeCancellable(request: okhttp3.Request): okhttp3.Response = suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation {
        call.cancel()
    }
    call.enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }
    })
}
