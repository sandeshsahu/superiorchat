package com.mobile.superiorutils.bot

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog
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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit

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
    @SerialName("link_preview_options") val linkPreviewOptions: LinkPreviewOptions? = null
)

/**
 * Centralized Telegram Bot API client.
 *
 * Single [OkHttpClient] instance and token sanitization logic shared across
 * all components (BotService, SnapshotEngine, BotCommands).
 */
object TelegramApi {

    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(65, TimeUnit.SECONDS)
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
        val url = apiUrl(token, "getUpdates") + "?offset=$offset&timeout=$timeout"
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
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { json.decodeFromString<FileResponse>(it) }
            } else null
        } catch (e: Exception) { null }
    }

    /** Helper to construct file download URL */
    fun getFileDownloadUrl(token: String, filePath: String): String {
        return "https://api.telegram.org/file/bot${sanitizeToken(token)}/$filePath"
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
        replyMarkup: String? = null
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
            val req = SendMessageRequest(chatId, text, parseMode, markupJson, LinkPreviewOptions(isDisabled = true))
            val jsonBody = json.encodeToString(req)
            val body = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(apiUrl(token, "sendMessage"))
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            var messageId: Long? = null
            if (!success) {
                val errorBody = response.body?.string()
                AppLog.log(LogCategory.NETWORK, "sendMessage failed: ${response.code} - $errorBody", com.mobile.superiorutils.utils.LogLevel.ERROR)
            } else {
                AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] " + text.take(200))
                val respBody = response.body?.string()
                if (respBody != null) {
                    try {
                        val jsonObject = json.parseToJsonElement(respBody).jsonObject
                        val result = jsonObject["result"]?.jsonObject
                        messageId = result?.get("message_id")?.jsonPrimitive?.long
                    } catch (e: Exception) {
                        AppLog.log(LogCategory.NETWORK, "Failed to parse sendMessage response: ${e.message}", com.mobile.superiorutils.utils.LogLevel.WARN)
                    }
                }
            }
            response.close()
            messageId
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendMessage error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            null
        }
    }



    /** Send a photo file. Returns true on success. */
    fun sendPhoto(token: String, chatId: String, file: File, caption: String? = null): Boolean {
        return try {
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendPhoto"))
                .post(builder.build())
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                AppLog.log(LogCategory.NETWORK, "sendPhoto failed: ${response.code}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            } else {
                AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Photo: ${file.name}" + (if (caption != null) " - ${caption.take(100)}" else ""))
            }
            response.close()
            success
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendPhoto error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            false
        }
    }

    /** Send a voice audio file. Returns true on success. */
    fun sendVoice(token: String, chatId: String, file: File, caption: String? = null): Boolean {
        return try {
            val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("voice", file.name, file.asRequestBody("audio/mp4".toMediaType()))
            if (caption != null) {
                builder.addFormDataPart("caption", caption)
                builder.addFormDataPart("parse_mode", "Markdown")
            }

            val request = Request.Builder()
                .url(apiUrl(token, "sendVoice"))
                .post(builder.build())
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                val errorBody = response.body?.string()
                AppLog.log(LogCategory.NETWORK, "sendVoice failed: ${response.code} - $errorBody", com.mobile.superiorutils.utils.LogLevel.ERROR)
            } else {
                AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Voice: ${file.name}")
            }
            response.close()
            success
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendVoice error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            false
        }
    }

    /** Send a document file. Returns true on success. */
    fun sendDocument(
        token: String,
        chatId: String,
        file: File,
        caption: String,
        parseMode: String = "Markdown"
    ): Boolean {
        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart("parse_mode", parseMode)
                .addFormDataPart("document", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val request = Request.Builder()
                .url(apiUrl(token, "sendDocument"))
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                val errorBody = response.body?.string()
                AppLog.log(LogCategory.NETWORK, "sendDocument failed: ${response.code} - $errorBody", com.mobile.superiorutils.utils.LogLevel.ERROR)
            } else {
                AppLog.log(LogCategory.BOT_ACTIVITY, "[SENTMSG] Document: ${file.name} - ${caption.take(100)}")
            }
            response.close()
            success
        } catch (e: Exception) {
            AppLog.log(LogCategory.NETWORK, "sendDocument error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            false
        }
    }




    /** Verifies BOTH local internet capabilities and Telegram API reachability. */
    fun isApiReachable(context: Context, token: String): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.activeNetwork == null) return false
        return getMe(token) != null
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
