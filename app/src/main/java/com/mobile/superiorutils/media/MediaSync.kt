package com.mobile.superiorutils.media

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.mobile.superiorutils.bot.TelegramApi
import com.mobile.superiorutils.core.LocalDb
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogLevel
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MediaSync {

    private val activeTransfers = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun enqueueDownload(
        context: Context,
        messageId: Long,
        fileId: String,
        mediaType: String
    ): UUID {
        AppLog.log(LogCategory.SYSTEM, "enqueueDownload: msgId=$messageId, fileId=$fileId, type=$mediaType")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(MediaWorker.KEY_MESSAGE_ID, messageId)
            .putString(MediaWorker.KEY_FILE_ID, fileId)
            .putString(MediaWorker.KEY_TRANSFER_TYPE, "DOWNLOAD")
            .putString(MediaWorker.KEY_MEDIA_TYPE, mediaType)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)

        // Also trigger foreground execution immediately for real-time sync!
        startDownloadImmediate(context, messageId, fileId, mediaType)

        return request.id
    }

    fun enqueueUpload(
        context: Context,
        messageId: Long,
        localPath: String,
        mediaType: String
    ): UUID {
        AppLog.log(LogCategory.SYSTEM, "enqueueUpload: msgId=$messageId, path=$localPath, type=$mediaType")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(MediaWorker.KEY_MESSAGE_ID, messageId)
            .putString(MediaWorker.KEY_LOCAL_PATH, localPath)
            .putString(MediaWorker.KEY_TRANSFER_TYPE, "UPLOAD")
            .putString(MediaWorker.KEY_MEDIA_TYPE, mediaType)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)

        // Also trigger foreground execution immediately for real-time sync!
        startUploadImmediate(context, messageId, localPath, mediaType)

        return request.id
    }

    fun startDownloadImmediate(context: Context, messageId: Long, fileId: String, mediaType: String) {
        val prefs = com.mobile.superiorutils.core.AppGraph.prefs
        val token = prefs.botToken
        if (token.isEmpty()) {
            AppLog.log(LogCategory.SYSTEM, "startDownloadImmediate: Bot token is empty, skipping.", LogLevel.WARN)
            return
        }

        synchronized(activeTransfers) {
            if (activeTransfers.containsKey(messageId)) {
                AppLog.log(LogCategory.SYSTEM, "startDownloadImmediate: Already active transfer for msgId=$messageId, skipping launch.")
                return
            }
            val job = scope.launch {
                performDownload(context, token, fileId, mediaType, messageId)
            }
            activeTransfers[messageId] = job
            job.invokeOnCompletion { activeTransfers.remove(messageId) }
        }
    }

    fun startUploadImmediate(context: Context, messageId: Long, localPath: String, mediaType: String) {
        val prefs = com.mobile.superiorutils.core.AppGraph.prefs
        val token = prefs.botToken
        val chatId = prefs.chatId
        if (token.isEmpty() || chatId.isEmpty()) {
            AppLog.log(LogCategory.SYSTEM, "startUploadImmediate: Missing token or chatId, skipping.", LogLevel.WARN)
            return
        }

        synchronized(activeTransfers) {
            if (activeTransfers.containsKey(messageId)) {
                AppLog.log(LogCategory.SYSTEM, "startUploadImmediate: Already active transfer for msgId=$messageId, skipping launch.")
                return
            }
            val job = scope.launch {
                performUpload(context, token, chatId, localPath, mediaType, messageId)
            }
            activeTransfers[messageId] = job
            job.invokeOnCompletion { activeTransfers.remove(messageId) }
        }
    }

    suspend fun performDownload(context: Context, token: String, fileId: String, mediaType: String, messageId: Long): Boolean {
        AppLog.log(LogCategory.SYSTEM, "performDownload: Starting for msgId=$messageId, type=$mediaType")
        
        val db = LocalDb.getDatabase(context)
        val msg = db.messageDao().getMessageById(messageId)
        if (msg != null && msg.status == MessageStatus.SENT && msg.mediaLocalPath != null) {
            val file = File(msg.mediaLocalPath)
            if (file.exists()) {
                AppLog.log(LogCategory.SYSTEM, "performDownload: File already exists locally, completing.")
                return true
            }
        }

        // Wait if another job is already active for this transfer
        val currentJob = kotlin.coroutines.coroutineContext[Job]
        val existingJob = activeTransfers[messageId]
        if (existingJob != null && existingJob.isActive && existingJob !== currentJob) {
            AppLog.log(LogCategory.SYSTEM, "performDownload: Waiting for active job to finish for msgId=$messageId")
            existingJob.join()
            val updatedMsg = db.messageDao().getMessageById(messageId)
            return updatedMsg?.status == MessageStatus.SENT
        }

        try {
            AppLog.log(LogCategory.NETWORK, "Fetching file path from Telegram for fileId=$fileId")
            val fileResponse = TelegramApi.getFile(token, fileId)
            val filePath = fileResponse?.result?.file_path ?: run {
                AppLog.log(LogCategory.NETWORK, "Failed to get file path from Telegram for fileId=$fileId", LogLevel.ERROR)
                markDownloadFailed(context, messageId)
                return false
            }

            val downloadUrl = TelegramApi.getFileDownloadUrl(token, filePath)
            AppLog.log(LogCategory.NETWORK, "Downloading file from: $downloadUrl")
            val request = Request.Builder().url(downloadUrl).build()

            val response = TelegramApi.client.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLog.log(LogCategory.NETWORK, "Telegram file download HTTP failure: ${response.code}", LogLevel.ERROR)
                markDownloadFailed(context, messageId)
                return false
            }

            val body = response.body ?: run {
                AppLog.log(LogCategory.NETWORK, "Telegram file download returned empty body", LogLevel.ERROR)
                markDownloadFailed(context, messageId)
                return false
            }

            val mediaDir = when (mediaType) {
                "photo" -> LocalDirs.getImageDir(context, isSent = false)
                "video" -> LocalDirs.getVideoDir(context, isSent = false)
                "voice" -> LocalDirs.getVoiceNoteDir(context, isSent = false)
                "audio" -> LocalDirs.getAudioDir(context, isSent = false)
                else -> LocalDirs.getDocumentDir(context, isSent = false)
            }

            val ext = filePath.substringAfterLast('.', "")
            val localFile = File(mediaDir, "${fileId}.${ext}")

            AppLog.log(LogCategory.SYSTEM, "Writing downloaded stream to local path: ${localFile.absolutePath}")
            body.byteStream().use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            val freshMsg = db.messageDao().getMessageById(messageId) ?: msg
            if (freshMsg != null) {
                db.messageDao().insertMessage(freshMsg.copy(mediaLocalPath = localFile.absolutePath, status = MessageStatus.SENT))
                AppLog.log(LogCategory.SYSTEM, "Successfully completed download & updated DB status to SENT for msgId=$messageId")
            } else {
                AppLog.log(LogCategory.SYSTEM, "Failed to update DB: Message not found for msgId=$messageId", LogLevel.ERROR)
            }
            return true
        } catch (e: Exception) {
            markDownloadFailed(context, messageId)
            AppLog.log(LogCategory.SYSTEM, "Exception in performDownload: ${e.message}", LogLevel.ERROR)
            return false
        }
    }

    suspend fun performUpload(context: Context, token: String, chatId: String, localPath: String, mediaType: String, messageId: Long): Boolean {
        AppLog.log(LogCategory.SYSTEM, "performUpload: Starting for msgId=$messageId, path=$localPath, type=$mediaType")
        
        val db = LocalDb.getDatabase(context)
        val msg = db.messageDao().getMessageById(messageId)
        if (msg != null && msg.status == MessageStatus.SENT) {
            AppLog.log(LogCategory.SYSTEM, "performUpload: Already marked as SENT in DB.")
            return true
        }

        val currentJob = kotlin.coroutines.coroutineContext[Job]
        val existingJob = activeTransfers[messageId]
        if (existingJob != null && existingJob.isActive && existingJob !== currentJob) {
            AppLog.log(LogCategory.SYSTEM, "performUpload: Waiting for active job to finish for msgId=$messageId")
            existingJob.join()
            val updatedMsg = db.messageDao().getMessageById(messageId)
            return updatedMsg?.status == MessageStatus.SENT
        }

        val file = File(localPath)
        if (!file.exists()) {
            AppLog.log(LogCategory.SYSTEM, "performUpload failed: Local file does not exist at $localPath", LogLevel.ERROR)
            return false
        }

        AppLog.log(LogCategory.NETWORK, "Uploading $mediaType file of size ${file.length()} bytes to chat $chatId")
        val success = when (mediaType) {
            "photo" -> TelegramApi.sendPhoto(token, chatId, file)
            "voice" -> TelegramApi.sendVoice(token, chatId, file)
            "document" -> TelegramApi.sendDocument(token, chatId, file, caption = "")
            else -> false
        }

        val freshMsg = db.messageDao().getMessageById(messageId) ?: msg
        if (freshMsg != null) {
            db.messageDao().insertMessage(freshMsg.copy(status = if (success) MessageStatus.SENT else MessageStatus.FAILED))
            AppLog.log(LogCategory.SYSTEM, "Updated DB status for msgId=$messageId to: ${if (success) "SENT" else "FAILED"}")
        }
        return success
    }

    private suspend fun markDownloadFailed(context: Context, messageId: Long) {
        try {
            val db = LocalDb.getDatabase(context)
            val msg = db.messageDao().getMessageById(messageId)
            if (msg != null) {
                db.messageDao().insertMessage(msg.copy(status = MessageStatus.FAILED))
                AppLog.log(LogCategory.SYSTEM, "Marked download as FAILED in DB for msgId=$messageId")
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to mark download as failed in DB: ${e.message}", LogLevel.ERROR)
        }
    }
}
