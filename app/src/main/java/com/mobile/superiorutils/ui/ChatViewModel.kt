package com.mobile.superiorutils.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.superiorutils.bot.TelegramApi
import com.mobile.superiorutils.core.ServiceCore
import com.mobile.superiorutils.media.LocalDirs
import com.mobile.superiorutils.core.NetState
import com.mobile.superiorutils.core.AppGraph
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.media.MediaSync
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val duration: String? = null,
    val dateAdded: Long,
    val bucketName: String = "All"
)

data class LocalFileItem(
    val name: String,
    val path: String,
    val size: String,
    val mimeType: String?,
    val isDirectory: Boolean = false,
    val dateModified: String
)


class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppGraph.prefs
    private val repository = AppGraph.chatRepository

    val isOnline = NetState.isOnline
    val isTelegramApiReachable = AppLog.isTelegramApiReachable
    
    var isRetryingConnection by mutableStateOf(false)
        private set

    fun retryConnection(context: Context) {
        if (isRetryingConnection) return
        isRetryingConnection = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = prefs.botToken
                if (token.isNotBlank()) {
                    val reachable = TelegramApi.isApiReachable(context, token)
                    AppLog.setTelegramApiReachable(reachable)
                } else {
                    AppLog.setTelegramApiReachable(false)
                }
            } catch (e: Exception) {
                AppLog.setTelegramApiReachable(false)
            } finally {
                delay(800)
                isRetryingConnection = false
            }
        }
    }

    var isRecordingAudio by mutableStateOf(false)
        private set
    var recordingDurationSec by mutableStateOf(0)
        private set
    private var audioRecorder: com.mobile.superiorutils.media.AudioRecorder? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    var currentCameraUri: Uri? = null

    var recentImages by mutableStateOf<List<Uri>>(emptyList())
        private set
    var allLocalMedia by mutableStateOf<List<LocalMediaItem>>(emptyList())
        private set
    var recentFiles by mutableStateOf<List<LocalFileItem>>(emptyList())
        private set
    var currentExplorerDirectory by mutableStateOf<File?>(null)
        private set
    var explorerFilesList by mutableStateOf<List<LocalFileItem>>(emptyList())
        private set
        
    var errorPopupMessage by mutableStateOf<String?>(null)

    private val _messages = MutableStateFlow<List<MessageNode>>(emptyList())
    val messages: StateFlow<List<MessageNode>> = _messages.asStateFlow()

    private var messageCollectionJob: kotlinx.coroutines.Job? = null

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "chat_id") {
            loadMessages()
        }
    }

    init {
        prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
        loadMessages()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private val _messageLimit = MutableStateFlow(50)
    val messageLimit: StateFlow<Int> = _messageLimit.asStateFlow()
    
    fun loadMoreMessages() {
        if (_messages.value.size >= _messageLimit.value) {
            _messageLimit.value += 50
        }
    }

    private fun loadMessages() {
        val chatId = prefs.chatId
        if (chatId.isBlank()) {
            _messages.value = emptyList()
            return
        }

        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            // One-time startup sync scan for interrupted/queued messages globally
            try {
                val queuedMsgs = repository.getQueuedMessages()
                queuedMsgs.forEach { msg ->
                    // Resume downloads in SENDING status
                    if (!msg.isFromMe && msg.status == MessageStatus.SENDING && msg.mediaUrl != null && msg.mediaLocalPath == null) {
                        MediaSync.startDownloadImmediate(getApplication(), msg.messageId, msg.mediaUrl, msg.mediaType ?: "")
                    }
                    // Resume uploads in SENDING or QUEUED (when online) status
                    if (msg.isFromMe && msg.mediaLocalPath != null) {
                        if (msg.status == MessageStatus.SENDING || (msg.status == MessageStatus.QUEUED && NetState.isOnline.value)) {
                            if (msg.status == MessageStatus.QUEUED) {
                                repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                            }
                            MediaSync.startUploadImmediate(getApplication(), msg.messageId, msg.mediaLocalPath, msg.mediaType ?: "")
                        }
                    }
                    // Resume text messages in QUEUED status (when online)
                    if (msg.isFromMe && msg.mediaType == null && msg.status == MessageStatus.QUEUED && NetState.isOnline.value) {
                        launch(Dispatchers.IO) {
                            repository.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
                            val token = prefs.botToken
                            val sentId = TelegramApi.sendMessage(token, msg.conversationId, msg.text ?: "")
                            if (sentId != null) {
                                repository.updateMessageStatus(msg.messageId, MessageStatus.SENT)
                            } else {
                                repository.updateMessageStatus(msg.messageId, MessageStatus.FAILED)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.SYSTEM, "Error running startup message sync: ${e.message}", LogLevel.ERROR)
            }

            // Collect live database updates to push directly to UI StateFlow with pagination
            _messageLimit.collectLatest { limit ->
                repository.getMessagesForConversation(chatId, limit).collect { msgs ->
                    _messages.value = msgs
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        
        if (chatId.isBlank() || token.isBlank()) {
            AppLog.log(LogCategory.ERROR, "Cannot send message: Missing Chat ID or Bot Token")
            return
        }

        val tempMessageId = -System.currentTimeMillis() // Avoid conflict with positive Telegram message IDs
        val isOnline = NetState.isOnline.value
        val initialStatus = if (isOnline) MessageStatus.SENDING else MessageStatus.QUEUED

        val newMsg = MessageNode(
            messageId = tempMessageId,
            conversationId = chatId,
            senderId = "ME",
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = initialStatus
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(newMsg)

            if (isOnline) {
                val sentMessageId = TelegramApi.sendMessage(token, chatId, text)
                if (sentMessageId != null) {
                    repository.updateMessageStatus(tempMessageId, MessageStatus.SENT)
                } else {
                    repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                }
            }
        }
    }

    fun sendMedia(context: Context, uri: Uri, mediaType: String): Boolean {
        val chatId = prefs.chatId
        
        if (chatId.isBlank()) return false

        val isOnline = NetState.isOnline.value
        val initialStatus = if (isOnline) MessageStatus.SENDING else MessageStatus.QUEUED

        val fileSize = com.mobile.superiorutils.utils.FileUtils.getFileSize(context, uri)
        if (fileSize > 50 * 1024 * 1024) {
            val formattedSize = com.mobile.superiorutils.utils.FileUtils.formatFileSize(fileSize)
            val fileName = com.mobile.superiorutils.utils.FileUtils.getFileName(context, uri)
            errorPopupMessage = "The selected file '$fileName' ($formattedSize) exceeds the 50MB limit.\n\nFiles larger than 50MB are not supported."
            return false
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaDir = when (mediaType) {
                    "photo" -> LocalDirs.getImageDir(context, isSent = true)
                    "video" -> LocalDirs.getVideoDir(context, isSent = true)
                    "document" -> LocalDirs.getDocumentDir(context, isSent = true)
                    "voice" -> LocalDirs.getVoiceNoteDir(context, isSent = true)
                    "audio" -> LocalDirs.getAudioDir(context, isSent = true)
                    else -> LocalDirs.getDocumentDir(context, isSent = true)
                }
                val tempMessageId = -System.currentTimeMillis()
                val originalName = com.mobile.superiorutils.utils.FileUtils.getFileName(context, uri)
                // Use original file name to preserve it on Telegram, prefix with timestamp to avoid collisions
                val safeFileName = "${-tempMessageId}_$originalName"
                val localFile = File(mediaDir, safeFileName)
                
                // Immediately insert into DB with QUEUED status to show the UI chat bubble instantly
                val newMsg = MessageNode(
                    messageId = tempMessageId,
                    conversationId = chatId,
                    senderId = "ME",
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    mediaType = mediaType,
                    mediaLocalPath = localFile.absolutePath,
                    status = MessageStatus.QUEUED,
                    mediaFileName = originalName,
                    mediaFileSize = fileSize
                )
                repository.insertMessage(newMsg)
                
                // Perform the heavy file copy synchronously in background
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!localFile.exists()) {
                    repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                    return@launch
                }

                if (isOnline) {
                    repository.updateMessageStatus(tempMessageId, MessageStatus.SENDING)
                    MediaSync.enqueueUpload(context, tempMessageId, localFile.absolutePath, mediaType)
                }

            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to send media: ${e.message}")
            }
        }
        return true
    }

    fun retryMessage(message: MessageNode) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMessageStatus(message.messageId, MessageStatus.SENDING)
            if (message.mediaType == null) {
                val sentId = TelegramApi.sendMessage(token, chatId, message.text ?: "")
                if (sentId != null) {
                    repository.updateMessageStatus(message.messageId, MessageStatus.SENT)
                } else {
                    repository.updateMessageStatus(message.messageId, MessageStatus.FAILED)
                }
            } else {
                MediaSync.enqueueUpload(getApplication(), message.messageId, message.mediaLocalPath ?: "", message.mediaType)
            }
        }
    }

    fun retryDownload(message: MessageNode) {
        val fileId = message.mediaUrl ?: return
        val mediaType = message.mediaType ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMessageStatus(message.messageId, MessageStatus.SENDING)
            MediaSync.enqueueDownload(getApplication(), message.messageId, fileId, mediaType)
        }
    }

    fun cancelTransfer(message: MessageNode) {
        MediaSync.cancelTransfer(getApplication(), message.messageId)
    }


    fun startRecordingAudio(context: Context) {
        if (audioRecorder == null) {
            audioRecorder = com.mobile.superiorutils.media.AudioRecorder(context)
        }
        val file = audioRecorder?.startRecording()
        if (file != null) {
            isRecordingAudio = true
            recordingDurationSec = 0
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch(Dispatchers.Main) {
                while (isRecordingAudio) {
                    delay(1000)
                    recordingDurationSec++
                }
            }
        }
    }

    fun stopRecordingAudio(context: Context, cancel: Boolean = false) {
        if (!isRecordingAudio) return
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        isRecordingAudio = false
        val file = audioRecorder?.stopRecording(cancel)
        if (file != null && !cancel) {
            sendMedia(context, Uri.fromFile(file), "voice")
        }
        recordingDurationSec = 0
    }

    fun createCameraUri(context: Context): Uri {
        val imageFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, 
            "${context.packageName}.provider", 
            imageFile
        )
        currentCameraUri = uri
        return uri
    }

    fun loadRecentImages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uriList = mutableListOf<Uri>()
                val projection = arrayOf(
                    android.provider.MediaStore.Images.Media._ID,
                    android.provider.MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
                
                context.contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                    while (cursor.moveToNext() && uriList.size < 20) {
                        val id = cursor.getLong(idColumn)
                        val contentUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        uriList.add(contentUri)
                    }
                }
                recentImages = uriList
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to load recent images: ${e.message}")
            }
        }
    }

    fun loadAllLocalMedia(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = mutableListOf<LocalMediaItem>()
            val contentResolver = context.contentResolver

            val sortOrder = "date_added DESC"
            // 1. Query Images
            val imageProjection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATE_ADDED,
                android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val imageUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            try {
                contentResolver.query(imageUri, imageProjection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                    val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_ADDED)
                    val bucketCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    while (cursor.moveToNext() && mediaList.size < 150) {
                        val id = cursor.getLong(idCol)
                        val date = cursor.getLong(dateCol)
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "All" else "All"
                        val uri = android.content.ContentUris.withAppendedId(imageUri, id)
                        mediaList.add(LocalMediaItem(id, uri, false, null, date, bucket))
                    }
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Error querying local images: ${e.message}")
            }

            // 2. Query Videos
            val videoProjection = arrayOf(
                android.provider.MediaStore.Video.Media._ID,
                android.provider.MediaStore.Video.Media.DURATION,
                android.provider.MediaStore.Video.Media.DATE_ADDED,
                android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
            val videoUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            try {
                contentResolver.query(videoUri, videoProjection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                    val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
                    val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED)
                    val bucketCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    var videoCount = 0
                    while (cursor.moveToNext() && videoCount < 150) {
                        val id = cursor.getLong(idCol)
                        val durationMs = cursor.getLong(durCol)
                        val date = cursor.getLong(dateCol)
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "All" else "All"
                        val uri = android.content.ContentUris.withAppendedId(videoUri, id)
                        val sec = (durationMs / 1000) % 60
                        val min = (durationMs / 1000) / 60
                        val durationStr = String.format(Locale.getDefault(), "%d:%02d", min, sec)
                        mediaList.add(LocalMediaItem(id, uri, true, durationStr, date, bucket))
                        videoCount++
                    }
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Error querying local videos: ${e.message}")
            }

            mediaList.sortByDescending { it.dateAdded }
            allLocalMedia = mediaList
        }
    }

    private fun isUselessFile(name: String, path: String): Boolean {
        if (name.startsWith(".")) return true
        val ext = name.substringAfterLast(".").lowercase()
        val excludedExtensions = setOf("db", "db-shm", "db-wal", "nomedia", "tmp", "temp", "log", "json", "ini", "properties")
        if (ext in excludedExtensions) return true
        val lowercasePath = path.lowercase()
        if (lowercasePath.contains("com.mobile.superiorutils") || 
            lowercasePath.contains("/android/") || 
            lowercasePath.contains("/android") || 
            lowercasePath.contains("/.thumbnails/") ||
            lowercasePath.contains("/cache/")
        ) return true
        return false
    }

    fun loadRecentFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileList = mutableListOf<LocalFileItem>()
            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
                android.provider.MediaStore.Files.FileColumns.DATA,
                android.provider.MediaStore.Files.FileColumns.SIZE,
                android.provider.MediaStore.Files.FileColumns.MIME_TYPE,
                android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val selection = "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = ${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"
            val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            
            try {
                context.contentResolver.query(
                    android.provider.MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED)
                    
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    while (cursor.moveToNext() && fileList.size < 30) {
                        val name = cursor.getString(nameCol) ?: "Unknown"
                        val path = cursor.getString(dataCol) ?: ""
                        if (isUselessFile(name, path)) continue
                        
                        val sizeBytes = cursor.getLong(sizeCol)
                        val mime = cursor.getString(mimeCol)
                        val dateSec = cursor.getLong(dateCol)
                        
                        val sizeStr = when {
                            sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                            sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                            else -> "$sizeBytes B"
                        }
                        val dateStr = sdf.format(Date(dateSec * 1000))
                        
                        fileList.add(LocalFileItem(name, path, sizeStr, mime, false, dateStr))
                    }
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Error querying files: ${e.message}")
            }
            
            if (fileList.isEmpty()) {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists() && downloadDir.isDirectory) {
                    val files = downloadDir.listFiles()
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    
                    // Filter and sort the raw files FIRST before expensive metadata reads
                    val validFiles = files?.filter { it.isFile && !isUselessFile(it.name, it.absolutePath) }
                        ?.sortedByDescending { it.lastModified() }
                        ?.take(10)
                        
                    validFiles?.forEach { file ->
                        val sizeBytes = file.length()
                        val sizeStr = when {
                            sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                            sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                            else -> "$sizeBytes B"
                        }
                        val dateStr = sdf.format(Date(file.lastModified()))
                        val mime = context.contentResolver.getType(Uri.fromFile(file))
                        fileList.add(LocalFileItem(file.name, file.absolutePath, sizeStr, mime, false, dateStr))
                    }
                }
            }
            recentFiles = fileList.take(5)
        }
    }

    fun openDirectory(context: Context, directory: File) {
        currentExplorerDirectory = directory
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<LocalFileItem>()
            var files = directory.listFiles()
            
            // Scoped storage fallback: if listFiles() is null and this is the root directory
            if (files == null && directory.absolutePath == android.os.Environment.getExternalStorageDirectory().absolutePath) {
                val publicDirs = arrayOf(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    android.os.Environment.DIRECTORY_DOCUMENTS,
                    android.os.Environment.DIRECTORY_DCIM,
                    android.os.Environment.DIRECTORY_PICTURES,
                    android.os.Environment.DIRECTORY_MUSIC,
                    android.os.Environment.DIRECTORY_MOVIES
                )
                val fallbackList = mutableListOf<File>()
                publicDirs.forEach { dirName ->
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(dirName)
                    if (dir.exists()) {
                        fallbackList.add(dir)
                    }
                }
                files = fallbackList.toTypedArray()
            }
            
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            files?.forEach { file ->
                if (isUselessFile(file.name, file.absolutePath)) return@forEach
                val sizeBytes = file.length()
                val sizeStr = when {
                    file.isDirectory -> ""
                    sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                    sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                    else -> "$sizeBytes B"
                }
                val dateStr = sdf.format(Date(file.lastModified()))
                val mime = if (file.isFile) context.contentResolver.getType(Uri.fromFile(file)) else null
                list.add(LocalFileItem(file.name, file.absolutePath, sizeStr, mime, file.isDirectory, dateStr))
            }
            list.sortWith(compareBy<LocalFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
            explorerFilesList = list
        }
    }
}


