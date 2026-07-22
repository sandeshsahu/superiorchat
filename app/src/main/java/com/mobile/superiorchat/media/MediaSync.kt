package com.mobile.superiorchat.media

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.bot.executeCancellable
import com.mobile.superiorchat.core.LocalDb
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogLevel
import com.mobile.superiorchat.utils.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MediaSync {

    private val activeTransfers = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uploadMutex = Mutex()
    private val downloadMutex = Mutex()

    // Progress tracking: messageId -> progress (0.0f to 1.0f)
    private val _transferProgress = ConcurrentHashMap<Long, MutableStateFlow<Float>>()
    
    // In-memory set of cancelled message IDs to prevent race conditions and auto-restarts
    private val cancelledTransfers = ConcurrentHashMap.newKeySet<Long>()

    fun getProgress(messageId: Long): StateFlow<Float> {
        return _transferProgress.getOrPut(messageId) { MutableStateFlow(0f) }
    }

    fun cancelTransfer(context: Context, messageId: Long) {
        AppLog.log(LogCategory.SYSTEM, "cancelTransfer: Cancelling transfer for msgId=$messageId")
        cancelledTransfers.add(messageId)
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("msg_$messageId")
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "cancelTransfer: WorkManager cancellation failed: ${e.message}")
        }
        val job = activeTransfers[messageId]
        job?.cancel()
        activeTransfers.remove(messageId)
        _transferProgress.remove(messageId)
        StatusFlow.unregisterTransfer(messageId)
        scope.launch {
            markDownloadFailed(context, messageId)
        }
    }

    fun enqueueDownload(
        context: Context,
        messageId: Long,
        fileId: String,
        mediaType: String
    ): UUID {
        cancelledTransfers.remove(messageId)
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
            .addTag("msg_$messageId")
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
        cancelledTransfers.remove(messageId)
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
            .addTag("msg_$messageId")
            .build()

        WorkManager.getInstance(context).enqueue(request)

        // Also trigger foreground execution immediately for real-time sync!
        startUploadImmediate(context, messageId, localPath, mediaType)

        return request.id
    }

    fun startDownloadImmediate(context: Context, messageId: Long, fileId: String, mediaType: String) {
        cancelledTransfers.remove(messageId)
        val prefs = com.mobile.superiorchat.core.AppGraph.prefs
        val token = prefs.botToken
        if (token.isEmpty()) {
            AppLog.log(LogCategory.SYSTEM, "startDownloadImmediate: Bot token is empty, skipping.", LogLevel.WARN)
            return
        }

        scope.launch {
            performDownload(context, token, fileId, mediaType, messageId)
        }
    }

    fun startUploadImmediate(context: Context, messageId: Long, localPath: String, mediaType: String) {
        cancelledTransfers.remove(messageId)
        val prefs = com.mobile.superiorchat.core.AppGraph.prefs
        val token = prefs.botToken
        val chatId = prefs.chatId
        if (token.isEmpty() || chatId.isEmpty()) {
            AppLog.log(LogCategory.SYSTEM, "startUploadImmediate: Missing token or chatId, skipping.", LogLevel.WARN)
            return
        }

        scope.launch {
            performUpload(context, token, chatId, localPath, mediaType, messageId)
        }
    }

    suspend fun performDownload(context: Context, token: String, fileId: String, mediaType: String, messageId: Long): Boolean {
        AppLog.log(LogCategory.SYSTEM, "performDownload: Starting for msgId=$messageId, type=$mediaType")
        if (cancelledTransfers.contains(messageId)) {
            AppLog.log(LogCategory.SYSTEM, "performDownload: Aborting due to cancellation for msgId=$messageId")
            return false
        }
        
        val db = LocalDb.getDatabase(context)
        val msg = db.messageDao().getMessageById(messageId)
        
        val actualFileId = fileId.substringBefore("|")
        val fileUniqueId = if (fileId.contains("|")) fileId.substringAfter("|") else null
        
        // Check if matching file already exists on local disk (in received or sent directory)
        val existingLocalFile = LocalDirs.findExistingMedia(
            context = context,
            mediaType = mediaType,
            fileUniqueId = fileUniqueId,
            messageId = messageId,
            fileName = msg?.mediaFileName,
            fileSize = msg?.mediaFileSize
        )

        if (existingLocalFile != null && existingLocalFile.exists()) {
            val relativePath = LocalDirs.toRelativePath(context, existingLocalFile)
            if (msg != null) {
                db.messageDao().insertMessage(msg.copy(mediaLocalPath = relativePath, status = MessageStatus.SENT))
            }
            AppLog.log(LogCategory.SYSTEM, "performDownload: Found existing local media file on disk at $relativePath; skipping network fetch.")
            return true
        }

        // Wait if another job is already active for this transfer, otherwise register this one
        val currentJob = kotlin.coroutines.coroutineContext[Job]
        var shouldWait = false
        var jobToWait: Job? = null

        synchronized(activeTransfers) {
            val existingJob = activeTransfers[messageId]
            if (existingJob != null && existingJob.isActive && existingJob !== currentJob) {
                shouldWait = true
                jobToWait = existingJob
            } else if (currentJob != null) {
                activeTransfers[messageId] = currentJob
                currentJob.invokeOnCompletion {
                    synchronized(activeTransfers) {
                        if (activeTransfers[messageId] === currentJob) {
                            activeTransfers.remove(messageId)
                        }
                    }
                }
            }
        }

        if (shouldWait && jobToWait != null) {
            AppLog.log(LogCategory.SYSTEM, "performDownload: Waiting for active job to finish for msgId=$messageId")
            jobToWait.join()
            val updatedMsg = db.messageDao().getMessageById(messageId)
            return updatedMsg?.status == MessageStatus.SENT
        }


        var tmpFile: File? = null
        try {
            AppLog.log(LogCategory.NETWORK, "Fetching file path from Telegram for fileId=$actualFileId")
            val fileResponse = TelegramApi.getFile(token, actualFileId)
            val filePath = fileResponse?.result?.file_path ?: run {
                AppLog.log(LogCategory.NETWORK, "Failed to get file path from Telegram for fileId=$actualFileId", LogLevel.ERROR)
                markDownloadFailed(context, messageId)
                return false
            }

            val downloadUrl = TelegramApi.getFileDownloadUrl(token, filePath)
            AppLog.log(LogCategory.NETWORK, "Downloading file from: $downloadUrl")
            val request = Request.Builder().url(downloadUrl).build()

            val response = TelegramApi.client.executeCancellable(request)
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
            val localFileName = if (!fileUniqueId.isNullOrBlank()) {
                val cleanName = msg?.mediaFileName ?: "${actualFileId}.${ext}"
                "${fileUniqueId}_$cleanName"
            } else if (msg != null && !msg.mediaFileName.isNullOrBlank()) {
                "${messageId}_${msg.mediaFileName}"
            } else {
                "${actualFileId}.${ext}"
            }
            val localFile = File(mediaDir, localFileName)
            tmpFile = File(mediaDir, "${localFileName}.tmp")

            AppLog.log(LogCategory.SYSTEM, "Streaming download to temp file: ${tmpFile.absolutePath}")
            val totalBytes = body.contentLength()
            val progressFlow = StatusFlow.registerTransfer(messageId, isUpload = false, mediaType = mediaType, fileName = msg?.mediaFileName ?: actualFileId, localPath = localFile.absolutePath)
            _transferProgress[messageId] = progressFlow
            
            downloadMutex.withLock {
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (cancelledTransfers.contains(messageId)) {
                                throw CancellationException("Download cancelled by user")
                            }
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val prog = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                progressFlow.value = prog
                                StatusFlow.updateProgress(messageId, prog)
                            }
                        }
                    }
                }

                // Atomic rename to final destination file on 100% completion
                if (tmpFile.exists()) {
                    if (localFile.exists()) localFile.delete()
                    tmpFile.renameTo(localFile)
                }
            }
            _transferProgress.remove(messageId)

            if (localFile.exists()) {
                val relativePath = LocalDirs.toRelativePath(context, localFile)
                val freshMsg = db.messageDao().getMessageById(messageId) ?: msg
                if (freshMsg != null) {
                    db.messageDao().insertMessage(freshMsg.copy(mediaLocalPath = relativePath, status = MessageStatus.SENT))
                    AppLog.log(LogCategory.SYSTEM, "Successfully completed download & updated DB status to SENT for msgId=$messageId")
                } else {
                    AppLog.log(LogCategory.SYSTEM, "Failed to update DB: Message not found for msgId=$messageId", LogLevel.ERROR)
                }
                return true
            } else {
                markDownloadFailed(context, messageId)
                return false
            }
        } catch (e: CancellationException) {
            AppLog.log(LogCategory.SYSTEM, "Download cancelled for msgId=$messageId")
            FileUtils.deleteQuietly(tmpFile)
            throw e
        } catch (e: Exception) {
            FileUtils.deleteQuietly(tmpFile)
            markDownloadFailed(context, messageId)
            AppLog.log(LogCategory.SYSTEM, "Exception in performDownload: ${e.message}", LogLevel.ERROR)
            return false
        } finally {
            cancelledTransfers.remove(messageId)
            StatusFlow.unregisterTransfer(messageId)
        }
    }

    suspend fun performUpload(context: Context, token: String, chatId: String, localPath: String, mediaType: String, messageId: Long): Boolean {
        AppLog.log(LogCategory.SYSTEM, "performUpload: Starting for msgId=$messageId, path=$localPath, type=$mediaType")
        if (cancelledTransfers.contains(messageId)) {
            AppLog.log(LogCategory.SYSTEM, "performUpload: Aborting due to cancellation for msgId=$messageId")
            return false
        }
        
        try {
            val db = LocalDb.getDatabase(context)
            val msg = db.messageDao().getMessageById(messageId)
            if (msg != null && msg.status == MessageStatus.SENT) {
                AppLog.log(LogCategory.SYSTEM, "performUpload: Already marked as SENT in DB.")
                return true
            }

            val currentJob = kotlin.coroutines.coroutineContext[Job]
            var shouldWait = false
            var jobToWait: Job? = null

            synchronized(activeTransfers) {
                val existingJob = activeTransfers[messageId]
                if (existingJob != null && existingJob.isActive && existingJob !== currentJob) {
                    shouldWait = true
                    jobToWait = existingJob
                } else if (currentJob != null) {
                    activeTransfers[messageId] = currentJob
                    currentJob.invokeOnCompletion {
                        synchronized(activeTransfers) {
                            if (activeTransfers[messageId] === currentJob) {
                                activeTransfers.remove(messageId)
                            }
                        }
                    }
                }
            }

            if (shouldWait && jobToWait != null) {
                AppLog.log(LogCategory.SYSTEM, "performUpload: Waiting for active job to finish for msgId=$messageId")
                jobToWait.join()
                val updatedMsg = db.messageDao().getMessageById(messageId)
                return updatedMsg?.status == MessageStatus.SENT
            }

            val file = LocalDirs.resolveFile(context, localPath)
            if (file == null || !file.exists()) {
                AppLog.log(LogCategory.SYSTEM, "performUpload failed: Local file does not exist at $localPath", LogLevel.ERROR)
                return false
            }

            AppLog.log(LogCategory.NETWORK, "Uploading $mediaType file of size ${file.length()} bytes to chat $chatId")
            val displayName = if (file.name.matches(Regex("^-?\\d+_.+"))) file.name.substringAfter("_") else file.name
            val progressFlow = StatusFlow.registerTransfer(messageId, isUpload = true, mediaType = mediaType, fileName = displayName, localPath = file.absolutePath)
            _transferProgress[messageId] = progressFlow
            val uploadResult = uploadMutex.withLock {
                val progressListener: (Long, Long) -> Unit = { bytesWritten, totalBytes ->
                    if (totalBytes > 0) {
                        val prog = (bytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        progressFlow.value = prog
                        StatusFlow.updateProgress(messageId, prog)
                    }
                }
                when (mediaType) {
                    "photo" -> TelegramApi.sendPhoto(token, chatId, file, onProgress = progressListener)
                    "video" -> TelegramApi.sendVideo(token, chatId, file, onProgress = progressListener)
                    "voice" -> TelegramApi.sendVoice(token, chatId, file, onProgress = progressListener)
                    "audio" -> TelegramApi.sendAudio(token, chatId, file, onProgress = progressListener)
                    "document" -> {
                        TelegramApi.sendDocument(token, chatId, file, caption = "", displayName = displayName, onProgress = progressListener)
                    }
                    else -> null
                }
            }
            _transferProgress.remove(messageId)

            val resultMessageId = uploadResult?.messageId
            val fileUniqueId = uploadResult?.fileUniqueId

            val freshMsg = db.messageDao().getMessageById(messageId) ?: msg
            if (freshMsg != null) {
                if (resultMessageId != null) {
                    // Rename local file on disk from temporary -msgId_name to real uniqueId_name
                    var finalFile = file
                    if (file.name.startsWith("-") || file.name.contains("_")) {
                        val cleanName = if (file.name.matches(Regex("^-?\\d+_.+"))) file.name.substringAfter("_") else file.name
                        val finalName = if (!fileUniqueId.isNullOrBlank()) {
                            "${fileUniqueId}_$cleanName"
                        } else {
                            "${resultMessageId}_$cleanName"
                        }
                        val renamedFile = File(file.parentFile, finalName)
                        if (file.renameTo(renamedFile)) {
                            finalFile = renamedFile
                            val oldRelative = LocalDirs.toRelativePath(context, file)
                            val newRelative = LocalDirs.toRelativePath(context, renamedFile)
                            db.messageDao().updateMediaLocalPaths(oldRelative, newRelative)
                        }
                    }
                    val relativePath = LocalDirs.toRelativePath(context, finalFile)
                    db.messageDao().deleteMessage(messageId)
                    db.messageDao().insertMessage(freshMsg.copy(messageId = resultMessageId, mediaUrl = if (fileUniqueId != null) "${freshMsg.mediaUrl}|$fileUniqueId" else freshMsg.mediaUrl, mediaLocalPath = relativePath, status = MessageStatus.SENT))
                    AppLog.log(LogCategory.SYSTEM, "Updated DB status for msgId=$messageId to SENT with real ID: $resultMessageId (Path: $relativePath)")
                } else {
                    val relativePath = LocalDirs.toRelativePath(context, file)
                    db.messageDao().insertMessage(freshMsg.copy(mediaLocalPath = relativePath, status = MessageStatus.FAILED))
                    AppLog.log(LogCategory.SYSTEM, "Updated DB status for msgId=$messageId to: FAILED")
                }
            }
            return resultMessageId != null
        } catch (e: CancellationException) {
            AppLog.log(LogCategory.SYSTEM, "Upload cancelled for msgId=$messageId")
            throw e
        } catch (e: Exception) {
            markDownloadFailed(context, messageId)
            AppLog.log(LogCategory.SYSTEM, "Exception in performUpload: ${e.message}", LogLevel.ERROR)
            return false
        } finally {
            cancelledTransfers.remove(messageId)
            StatusFlow.unregisterTransfer(messageId)
        }
    }

    private suspend fun markDownloadFailed(context: Context, messageId: Long) {
        try {
            val db = LocalDb.getDatabase(context)
            val msg = db.messageDao().getMessageById(messageId)
            if (msg != null) {
                // Keep all media details (fileName, size, mediaUrl, mediaType) so UI thumbnail/bubble stays intact!
                db.messageDao().insertMessage(msg.copy(status = MessageStatus.FAILED))
                AppLog.log(LogCategory.SYSTEM, "Marked download/upload as FAILED in DB for msgId=$messageId")
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to mark transfer as failed in DB: ${e.message}", LogLevel.ERROR)
        } finally {
            _transferProgress.remove(messageId)
        }
    }

    suspend fun resumeInterruptedTransfers(context: Context, repository: com.mobile.superiorchat.data.repository.AppRepository) {
        try {
            val queuedMsgs = repository.getQueuedMessages()
            val sendingMsgs = com.mobile.superiorchat.core.LocalDb.getDatabase(context).messageDao().getMessagesByStatus(MessageStatus.SENDING)
            val allInterrupted = queuedMsgs + sendingMsgs

            allInterrupted.forEach { msg ->
                val resolvedFile = LocalDirs.resolveFile(context, msg.mediaLocalPath)
                val fileExists = resolvedFile != null && resolvedFile.exists()

                // Resume downloads in SENDING status
                if (!msg.isFromMe && msg.status == MessageStatus.SENDING && msg.mediaUrl != null && !fileExists) {
                    startDownloadImmediate(context, msg.messageId, msg.mediaUrl, msg.mediaType ?: "")
                }
                // Resume uploads in SENDING or QUEUED (when online) status
                if (msg.isFromMe && msg.mediaLocalPath != null) {
                    if (msg.status == MessageStatus.SENDING || (msg.status == MessageStatus.QUEUED && com.mobile.superiorchat.core.NetState.isOnline.value)) {
                        if (msg.status == MessageStatus.QUEUED) {
                            repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                        }
                        startUploadImmediate(context, msg.messageId, msg.mediaLocalPath, msg.mediaType ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Error running startup media sync: ${e.message}", LogLevel.ERROR)
        }
    }

    suspend fun syncTargetProfile(context: Context, token: String, chatId: String) {
        if (chatId.isBlank() || token.isBlank()) return
        try {
            StatusFlow.reportStatus(SyncState.SYNCING_PROFILE, "Checking profile details...")
            val chatResponse = TelegramApi.getChat(token, chatId)
            val chat = chatResponse?.result
            
            if (chat == null) {
                AppLog.log(LogCategory.SYSTEM, "Failed to fetch target profile, keeping existing data.")
                StatusFlow.reportStatus(SyncState.ERROR, "Failed to sync profile")
                return
            }
            
            val title = chat.first_name ?: chat.title ?: "Unknown"
            val username = chat.username ?: ""
            val type = chat.type
            val bio = chat.bio ?: chat.description
            val inviteLink = chat.invite_link
            val hasProtectedContent = chat.has_protected_content ?: false
            val isForum = chat.is_forum ?: false
            
            val photoUniqueId = chat.photo?.big_file_unique_id ?: ""
            val bigFileId = chat.photo?.big_file_id
            
            val repository = com.mobile.superiorchat.core.AppGraph.appRepository
            val existingProfile = repository.getProfileSync(chatId)
            var localPath = existingProfile?.profilePhotoPath ?: ""
            
            if (photoUniqueId.isNotEmpty() && photoUniqueId != existingProfile?.photoUniqueId) {
                if (bigFileId != null) {
                    StatusFlow.reportStatus(SyncState.SYNCING_PROFILE, "Updating profile picture...")
                    val fileResponse = TelegramApi.getFile(token, bigFileId)
                    val filePath = fileResponse?.result?.file_path
                    if (filePath != null) {
                        val downloadUrl = TelegramApi.getFileDownloadUrl(token, filePath)
                        val cacheDir = java.io.File(context.filesDir, "profiles")
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val destFile = java.io.File(cacheDir, "profile_${chatId}_${photoUniqueId}.jpg")
                        val success = TelegramApi.downloadFileToLocal(downloadUrl, destFile)
                        if (success) {
                            if (localPath.isNotEmpty()) {
                                val oldFile = java.io.File(localPath)
                                if (oldFile.exists() && oldFile.absolutePath != destFile.absolutePath) {
                                    oldFile.delete()
                                }
                            }
                            localPath = destFile.absolutePath
                        }
                    }
                }
            } else if (photoUniqueId.isEmpty()) {
                localPath = ""
            }
            
            val newProfile = com.mobile.superiorchat.data.entity.UserProfile(
                chatId = chatId,
                title = title,
                username = username,
                type = type,
                profilePhotoPath = localPath,
                photoUniqueId = photoUniqueId,
                bio = bio,
                inviteLink = inviteLink,
                hasProtectedContent = hasProtectedContent,
                isForum = isForum
            )
            repository.insertProfile(newProfile)

            // Update ChatNode with pinnedMessageId
            val pinnedMsgId = chat.pinned_message?.message_id
            val chatNode = repository.getChatSync(chatId)
            if (chatNode != null) {
                repository.updateChat(chatNode.copy(pinnedMessageId = pinnedMsgId))
            }

            val isUnchanged = existingProfile != null &&
                              title == existingProfile.title && 
                              username == existingProfile.username && 
                              photoUniqueId == existingProfile.photoUniqueId &&
                              bio == existingProfile.bio &&
                              inviteLink == existingProfile.inviteLink &&
                              hasProtectedContent == existingProfile.hasProtectedContent &&
                              isForum == existingProfile.isForum
            if (isUnchanged) {
                StatusFlow.reportStatus(SyncState.SUCCESS, "No changes")
            } else {
                StatusFlow.reportStatus(SyncState.SUCCESS, "Profile updated!")
            }
            
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to sync target profile: ${e.message}")
            StatusFlow.reportStatus(SyncState.ERROR, "Failed to sync profile")
        }
    }
}
