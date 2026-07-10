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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PermissionStatus(
    val hasPostNotifs: Boolean = false,
    val hasIgnoreBattery: Boolean = false,
    val isInternetConnected: Boolean = false
) {
    val allPermissionsGranted: Boolean
        get() = hasPostNotifs && hasIgnoreBattery
}

/**
 * ViewModel for SuperiorChat. Owns credential state and basic permissions.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppGraph.prefs

    val isNetworkAvailable: StateFlow<Boolean> = NetState.isOnline

    private var prefListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    init {
        prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            // Update preferences if needed
        }
        prefs.sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)

        viewModelScope.launch {
            NetState.isOnline.collectLatest { isOnline ->
                checkTelegramConnection()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefListener?.let {
            prefs.sharedPreferences.unregisterOnSharedPreferenceChangeListener(it)
        }
    }

    fun checkTelegramConnection() {
        if (!isNetworkAvailable.value) {
            AppLog.setTelegramApiReachable(false)
            return
        }
        val token = prefs.botToken
        if (token.isBlank()) {
            AppLog.setTelegramApiReachable(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val reachable = TelegramApi.isApiReachable(getApplication<Application>(), token)
            AppLog.setTelegramApiReachable(reachable)
        }
    }

    // -- Credential State --
    var botToken by mutableStateOf(prefs.botToken)
    var chatId by mutableStateOf(prefs.chatId)

    val hasCredentials: Boolean
        get() = botToken.trim().matches(Regex("^[0-9]+:[a-zA-Z0-9_-]+$")) && 
                chatId.trim().matches(Regex("^-?[0-9]+$"))

    // -- Permissions State --
    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    // -- Service/System Status (observed from AppLog) --
    val isServiceRunning = AppLog.isServiceRunning
    val isTelegramApiReachable = AppLog.isTelegramApiReachable

    // -------------------------------------------------------------------------
    //  PERMISSIONS
    // -------------------------------------------------------------------------

    fun refreshPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            
            val hasPostNotifs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val hasIgnoreBattery = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isInternetConnected = cm.activeNetwork != null

            _permissionStatus.value = PermissionStatus(
                hasPostNotifs = hasPostNotifs,
                hasIgnoreBattery = hasIgnoreBattery,
                isInternetConnected = isInternetConnected
            )
        }
    }

    // -------------------------------------------------------------------------
    //  ACTIONS
    // -------------------------------------------------------------------------

    fun saveCredentials() {
        val token = botToken.trim()
        val chat = chatId.trim()
        
        prefs.botToken = token
        prefs.chatId = chat
        
        botToken = token
        chatId = chat

        if (prefs.isConfigured) {
            ServiceCore.ensureRunning(getApplication<Application>())
        }
    }
}


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
    private var audioRecorder: com.mobile.superiorutils.audio.AudioRecorder? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    var currentCameraUri: Uri? = null

    var recentImages by mutableStateOf<List<Uri>>(emptyList())
        private set

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

    private fun loadMessages() {
        val chatId = prefs.chatId
        if (chatId.isBlank()) {
            _messages.value = emptyList()
            return
        }

        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            // One-time startup sync scan for interrupted/queued messages
            try {
                val firstList = repository.getMessagesForConversation(chatId).first()
                firstList.forEach { msg ->
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

            // Collect live database updates to push directly to UI StateFlow
            repository.getMessagesForConversation(chatId).collect { msgs ->
                _messages.value = msgs
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

    fun sendMedia(context: Context, uri: Uri, mediaType: String) {
        val chatId = prefs.chatId
        
        if (chatId.isBlank()) return

        val isOnline = NetState.isOnline.value
        val initialStatus = if (isOnline) MessageStatus.SENDING else MessageStatus.QUEUED

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
                val ext = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
                val tempMessageId = -System.currentTimeMillis()
                val localFile = File(mediaDir, "upload_${-tempMessageId}.$ext")
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!localFile.exists()) return@launch

                val newMsg = MessageNode(
                    messageId = tempMessageId,
                    conversationId = chatId,
                    senderId = "ME",
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    mediaType = mediaType,
                    mediaLocalPath = localFile.absolutePath,
                    status = initialStatus
                )
                repository.insertMessage(newMsg)

                if (isOnline) {
                    MediaSync.enqueueUpload(context, tempMessageId, localFile.absolutePath, mediaType)
                }

            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "Failed to send media: ${e.message}")
            }
        }
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


    fun startRecordingAudio(context: Context) {
        if (audioRecorder == null) {
            audioRecorder = com.mobile.superiorutils.audio.AudioRecorder(context)
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
}
