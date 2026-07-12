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

        // Check if the transfer has already completed, failed, or been cancelled by the user.
        val db = LocalDb.getDatabase(context)
        val msg = db.messageDao().getMessageById(messageId)
        if (msg == null) {
            return@withContext Result.failure()
        }
        if (msg.status == MessageStatus.SENT || msg.status == MessageStatus.FAILED) {
            AppLog.log(LogCategory.SYSTEM, "MediaWorker: msgId=$messageId already has status ${msg.status}, skipping.")
            return@withContext Result.success()
        }

        try {
            val success = if (transferType == "DOWNLOAD" && fileId != null && mediaType != null) {
                MediaSync.performDownload(context, token, fileId, mediaType, messageId)
            } else if (transferType == "UPLOAD" && localPath != null && mediaType != null) {
                MediaSync.performUpload(context, token, chatId, localPath, mediaType, messageId)
            } else {
                false
            }

            if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "MediaWorker Error: ${e.message}", com.mobile.superiorutils.utils.LogLevel.ERROR)
            Result.failure()
        }
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_FILE_ID = "file_id"
        const val KEY_TRANSFER_TYPE = "transfer_type"
        const val KEY_MEDIA_TYPE = "media_type"
        const val KEY_LOCAL_PATH = "local_path"
    }
}
