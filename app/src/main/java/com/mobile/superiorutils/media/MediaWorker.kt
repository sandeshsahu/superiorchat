package com.mobile.superiorutils.media

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.mobile.superiorutils.bot.TelegramApi
import com.mobile.superiorutils.bot.FileResponse
import com.mobile.superiorutils.core.LocalDb
import com.mobile.superiorutils.data.Prefs
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import okhttp3.Request

class MediaWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val messageId = inputData.getLong(KEY_MESSAGE_ID, -1)
        val fileId = inputData.getString(KEY_FILE_ID)
        val transferType = inputData.getString(KEY_TRANSFER_TYPE) // "DOWNLOAD" or "UPLOAD"
        val mediaType = inputData.getString(KEY_MEDIA_TYPE) // "photo", "video", "document"
        val localPath = inputData.getString(KEY_LOCAL_PATH)

        if (messageId == -1L || transferType == null) {
            return@withContext Result.failure()
        }

        val prefs = Prefs.getInstance(context)
        val token = prefs.botToken
        val chatId = prefs.chatId
        if (token.isEmpty() || chatId.isEmpty()) return@withContext Result.failure()

        try {
            if (transferType == "DOWNLOAD" && fileId != null) {
                downloadFile(token, fileId, messageId)
            } else if (transferType == "UPLOAD" && localPath != null) {
                uploadFile(token, chatId, localPath, mediaType, messageId)
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "MediaWorker Error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            Result.retry()
        }
    }

    private suspend fun downloadFile(token: String, fileId: String, messageId: Long): Result {
        try {
            val fileResponse = TelegramApi.getFile(token, fileId)
            val filePath = fileResponse?.result?.file_path ?: run {
                markDownloadFailed(messageId)
                return Result.failure()
            }

            val downloadUrl = TelegramApi.getFileDownloadUrl(token, filePath)
            val request = Request.Builder().url(downloadUrl).build()

            val response = TelegramApi.client.newCall(request).execute()
            if (!response.isSuccessful) {
                markDownloadFailed(messageId)
                return Result.retry()
            }

            val body = response.body ?: run {
                markDownloadFailed(messageId)
                return Result.failure()
            }

            // Determine Directory from DB Message
            val db = LocalDb.getDatabase(context)
            val msg = db.messageDao().getMessageById(messageId)
            val mediaTypeStr = msg?.mediaType ?: ""
            
            val mediaDir = when (mediaTypeStr) {
                "photo" -> LocalDirs.getImageDir(context, isSent = false)
                "video" -> LocalDirs.getVideoDir(context, isSent = false)
                "voice" -> LocalDirs.getVoiceNoteDir(context, isSent = false)
                "audio" -> LocalDirs.getAudioDir(context, isSent = false)
                else -> LocalDirs.getDocumentDir(context, isSent = false)
            }

            val ext = filePath.substringAfterLast('.', "")
            // Use original file name if it's a document and we can parse it? For now use fileId
            val localFile = File(mediaDir, "${fileId}.${ext}")

            body.byteStream().use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Update Database
            if (msg != null) {
                db.messageDao().insertMessage(msg.copy(mediaLocalPath = localFile.absolutePath, status = MessageStatus.SENT)) // 1 = Sent/Downloaded
            }

            return Result.success()
        } catch (e: Exception) {
            markDownloadFailed(messageId)
            AppLog.log(LogCategory.SYSTEM, "Failed to download file: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            return Result.failure()
        }
    }

    private suspend fun markDownloadFailed(messageId: Long) {
        try {
            val db = LocalDb.getDatabase(context)
            val msg = db.messageDao().getMessageById(messageId)
            if (msg != null) {
                db.messageDao().insertMessage(msg.copy(status = MessageStatus.FAILED))
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to mark download as failed in DB: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
        }
    }

    private suspend fun uploadFile(token: String, chatId: String, localPath: String, mediaType: String?, messageId: Long): Result {
        val file = File(localPath)
        if (!file.exists()) return Result.failure()

        val success = when (mediaType) {
            "photo" -> TelegramApi.sendPhoto(token, chatId, file)
            "voice" -> TelegramApi.sendVoice(token, chatId, file)
            "document" -> TelegramApi.sendDocument(token, chatId, file, caption = "")
            // implement others if needed
            else -> false
        }

        val db = LocalDb.getDatabase(context)
        val msg = db.messageDao().getMessageById(messageId)
        if (msg != null) {
            db.messageDao().insertMessage(msg.copy(status = if (success) MessageStatus.SENT else MessageStatus.FAILED)) // 1=Sent, 2=Failed
        }

        return if (success) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_FILE_ID = "file_id"
        const val KEY_TRANSFER_TYPE = "transfer_type"
        const val KEY_MEDIA_TYPE = "media_type"
        const val KEY_LOCAL_PATH = "local_path"
    }
}
