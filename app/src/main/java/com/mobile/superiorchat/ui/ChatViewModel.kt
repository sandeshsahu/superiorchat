package com.mobile.superiorchat.ui

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
import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.core.ServiceCore
import com.mobile.superiorchat.media.LocalDirs
import com.mobile.superiorchat.core.NetState
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.media.MediaSync
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogLevel
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.mobile.superiorchat.ui.components.ScrollEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.mobile.superiorchat.data.repository.LocalMediaItem
import com.mobile.superiorchat.data.repository.LocalFileItem


class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppGraph.prefs
    private val repository = AppGraph.appRepository

    private var lastMessageTime = 0L

    private fun getNextMessageTime(): Long {
        var currentTime = System.currentTimeMillis()
        if (currentTime <= lastMessageTime) {
            currentTime = lastMessageTime + 1
        }
        lastMessageTime = currentTime
        return currentTime
    }

    val isOnline = NetState.isOnline
    val isTelegramApiReachable = AppLog.isTelegramApiReachable
    val isBotTokenInvalid = AppLog.isBotTokenInvalid
    
    var isCredentialsEmpty by mutableStateOf(prefs.botToken.isBlank() || prefs.chatId.isBlank())
        private set
    
    var isRetryingConnection by mutableStateOf(false)
        private set

    var replyingToMessage by mutableStateOf<MessageNode?>(null)
        private set

    var editingMessage by mutableStateOf<MessageNode?>(null)
        private set

    // Tracks the last emoji the user reacted with
    var lastUsedEmoji by mutableStateOf<String?>(null)
        private set

    var sortedEmojis = androidx.compose.runtime.mutableStateListOf<String>()
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val usages = repository.getEmojiUsage()
            if (usages.isNotEmpty()) {
                val lastUsed = usages.maxByOrNull { it.lastUsedAt }?.emoji
                lastUsedEmoji = lastUsed
            }
            updateSortedEmojis(usages)
        }
    }

    private fun updateSortedEmojis(usages: List<com.mobile.superiorchat.data.entity.EmojiUsage>) {
        val allEmojis = listOf("👍", "❤️", "🔥", "🤣", "👏", "😁", "🤔", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩", "🙏", "🤡", "🥱", "😍", "💯", "💔", "😐", "🍓", "😈", "😴", "😭", "👻", "👀", "🤝", "😡", "😘")
        val usageMap = usages.associate { it.emoji to it.usageCount }
        
        val sorted = allEmojis.sortedByDescending { usageMap[it] ?: 0 }.toMutableList()
        lastUsedEmoji?.let { last ->
            sorted.remove(last)
            sorted.add(0, last)
        }
        
        viewModelScope.launch(Dispatchers.Main) {
            sortedEmojis.clear()
            sortedEmojis.addAll(sorted)
        }
    }


    @JvmName("setReplyingMsg")
    fun setReplyingToMessage(message: MessageNode?) {
        replyingToMessage = message
        if (message != null) {
            editingMessage = null // Cannot edit and reply at the same time
        }
    }

    @JvmName("setEditingMsg")
    fun setEditingMessage(message: MessageNode?) {
        editingMessage = message
        if (message != null) {
            replyingToMessage = null // Cannot edit and reply at the same time
        }
    }

    private var lastReactionTime = 0L

    /**
     * Toggle a reaction emoji on a message.
     * If the message already has this emoji, it will be removed (toggle off).
     * Optimistically updates the local DB; rolls back silently on API failure.
     */
    fun sendReaction(message: MessageNode, emoji: String) {
        val now = System.currentTimeMillis()
        if (now - lastReactionTime < 400) return // Debounce rapid accidental double-taps on the badge
        lastReactionTime = now

        val token = prefs.botToken
        val chatId = prefs.chatId
        if (token.isBlank() || chatId.isBlank()) return

        // Parse current reactions using structured JSON model
        val currentData = com.mobile.superiorchat.data.entity.ReactionData.parse(message.reactions)
        val myReactions = currentData.me.toMutableList()

        val isToggleOff = myReactions.contains(emoji)
        if (isToggleOff) {
            myReactions.remove(emoji)
        } else {
            // Usually, standard users have 1 reaction per message. Let's clear previous to mimic standard behavior
            myReactions.clear()
            myReactions.add(emoji)
            lastUsedEmoji = emoji
        }
        
        val newData = currentData.copy(me = myReactions)
        val newJson = com.mobile.superiorchat.data.entity.ReactionData.toJson(newData)

        // Optimistic local update
        viewModelScope.launch(Dispatchers.IO) {
            if (!isToggleOff) {
                repository.recordEmojiUsage(emoji)
                val usages = repository.getEmojiUsage()
                updateSortedEmojis(usages)
            }
            repository.updateMessageReactions(message.messageId, newJson)
            if (NetState.isOnline.value) {
                // Send only the last/most recent emoji to the API
                val apiEmoji = myReactions.lastOrNull() ?: ""
                val success = TelegramApi.setMessageReaction(token, chatId, message.messageId, apiEmoji)
                if (!success) {
                    // Telegram has strict rate limits for reactions (429 Too Many Requests).
                    // We DO NOT roll back the local database anymore to keep the UI feeling fluid.
                    // The Android app stays fast, even if Telegram drops a rapid-fire reaction.
                }
            }
        }
    }

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
    private var audioRecorder: com.mobile.superiorchat.media.AudioRecorder? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    var currentCameraUri: Uri? = null

    var recentImages by mutableStateOf<List<Uri>>(emptyList())
        private set
    var allLocalMedia by mutableStateOf<List<LocalMediaItem>?>(null)
        private set
    var recentFiles by mutableStateOf<List<LocalFileItem>>(emptyList())
        private set
    var currentExplorerDirectory by mutableStateOf<File?>(null)
        private set
    var explorerFilesList by mutableStateOf<List<LocalFileItem>>(emptyList())
        private set
        
    var errorPopupMessage by mutableStateOf<String?>(null)

    // ── Multi-selection state ──────────────────────────────────────────────────
    var isInSelectionMode by mutableStateOf(false)
        private set
    var selectedMessageIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    fun enterSelectionMode(message: MessageNode) {
        isInSelectionMode = true
        selectedMessageIds = setOf(message.messageId)
    }

    fun toggleMessageSelection(message: MessageNode) {
        val id = message.messageId
        selectedMessageIds = if (selectedMessageIds.contains(id)) {
            selectedMessageIds - id
        } else {
            selectedMessageIds + id
        }
        if (selectedMessageIds.isEmpty()) {
            isInSelectionMode = false
        }
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedMessageIds = emptySet()
    }

    var activePopupMessageId by mutableStateOf<Long?>(null)
        private set

    fun showContextMenu(messageId: Long) {
        activePopupMessageId = messageId
    }

    fun hideContextMenu() {
        activePopupMessageId = null
    }

    private val _undoDeleteEvent = MutableSharedFlow<MessageNode>()
    val undoDeleteEvent = _undoDeleteEvent.asSharedFlow()

    private val _undoBulkDeleteEvent = MutableSharedFlow<List<MessageNode>>()
    val undoBulkDeleteEvent = _undoBulkDeleteEvent.asSharedFlow()

    fun deleteMessageForMe(message: MessageNode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(message.messageId)
            _undoDeleteEvent.emit(message)
        }
    }

    fun undoDeleteMessage(message: MessageNode) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(message)
        }
    }

    fun deleteSelectedMessagesForMe(messagesToDelete: List<MessageNode>) {
        val selected = messagesToDelete.filter { selectedMessageIds.contains(it.messageId) }
        exitSelectionMode()
        viewModelScope.launch(Dispatchers.IO) {
            selected.forEach { message ->
                repository.deleteMessage(message.messageId)
            }
            _undoBulkDeleteEvent.emit(selected)
        }
    }

    fun undoBulkDeleteMessages(messages: List<MessageNode>) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.forEach { message ->
                repository.insertMessage(message)
            }
        }
    }

    fun deleteSelectedMessages(messagesToDelete: List<MessageNode>) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return
        val selected = messagesToDelete.filter { selectedMessageIds.contains(it.messageId) }
        exitSelectionMode()
        viewModelScope.launch(Dispatchers.IO) {
            var anyFailed = false
            selected.forEach { message ->
                repository.deleteMessage(message.messageId)
                var success = false
                if (NetState.isOnline.value) {
                    success = TelegramApi.deleteMessage(token, chatId, message.messageId)
                }
                if (!success) {
                    AppLog.log(LogCategory.ERROR, "Failed to bulk-delete message ${message.messageId} via API")
                    repository.insertMessage(message)
                    anyFailed = true
                }
            }
            // Report a single consolidated error if any delete failed (messages were re-inserted)
            if (anyFailed) {
                StatusFlow.reportStatus(SyncState.ERROR, "Some messages could not be deleted")
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────────


    private val _messages = MutableStateFlow<List<MessageNode>>(emptyList())
    val messages: StateFlow<List<MessageNode>> = _messages.asStateFlow()

    private val _currentPinnedMessage = MutableStateFlow<MessageNode?>(null)
    val currentPinnedMessage: StateFlow<MessageNode?> = _currentPinnedMessage.asStateFlow()

    private val _userProfile = MutableStateFlow<com.mobile.superiorchat.data.entity.UserProfile?>(null)
    val userProfile: StateFlow<com.mobile.superiorchat.data.entity.UserProfile?> = _userProfile.asStateFlow()

    private val _scrollEvents = MutableSharedFlow<ScrollEvent>(extraBufferCapacity = 16)
    val scrollEvents: SharedFlow<ScrollEvent> = _scrollEvents.asSharedFlow()

    var hasUnreadMessages by mutableStateOf(false)

    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    fun requestJumpToBottom() {
        viewModelScope.launch {
            _scrollEvents.emit(ScrollEvent.JumpToBottomRequested)
        }
    }

    private var messageCollectionJob: kotlinx.coroutines.Job? = null

    private val _messageLimit = MutableStateFlow(50)
    val messageLimit: StateFlow<Int> = _messageLimit.asStateFlow()

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "bot_token" || key == "chat_id") {
            isCredentialsEmpty = prefs.botToken.isBlank() || prefs.chatId.isBlank()
        }
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


    
    fun loadMoreMessages() {
        if (_messages.value.size >= _messageLimit.value) {
            _messageLimit.value += 50
        }
    }

    private fun loadMessages() {
        val chatId = prefs.chatId
        if (chatId.isBlank()) {
            _messages.value = emptyList()
            _isLoadingInitial.value = false
            return
        }

        _isLoadingInitial.value = true
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            // One-time startup sync scan for interrupted/queued messages globally
            launch {
                MediaSync.resumeInterruptedTransfers(getApplication(), repository)
            }

            launch {
                repository.getProfile(chatId).collectLatest { profile ->
                    _userProfile.value = profile
                }
            }
            
            launch {
                repository.getChat(chatId).collectLatest { chat ->
                    if (chat?.pinnedMessageId != null) {
                        _currentPinnedMessage.value = repository.getMessageById(chat.pinnedMessageId)
                    } else {
                        _currentPinnedMessage.value = null
                    }
                }
            }

            // Collect live database updates to push directly to UI StateFlow with pagination
            var previousMsgs: List<MessageNode>? = null
            _messageLimit.collectLatest { limit ->
                repository.getMessagesForConversation(chatId, limit).collect { msgs ->
                    val oldMsgs = previousMsgs
                    previousMsgs = msgs
                    _messages.value = msgs
                    _isLoadingInitial.value = false

                    if (oldMsgs != null) {
                        val newestOld = oldMsgs.lastOrNull()
                        val newestNew = msgs.lastOrNull()
                        if (newestNew != null && newestNew.messageId != newestOld?.messageId) {
                            _scrollEvents.emit(ScrollEvent.NewMessageInserted(isFromMe = newestNew.isFromMe))
                        } else if (msgs.size > oldMsgs.size) {
                            _scrollEvents.emit(ScrollEvent.OlderMessagesLoaded)
                        }
                    } else if (msgs.isNotEmpty()) {
                        _scrollEvents.emit(ScrollEvent.JumpToBottomRequested)
                    }
                }
            }
        }
    }

    fun forceSyncProfile(context: Context) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            MediaSync.syncTargetProfile(context, token, chatId)
        }
    }

    fun pinMessage(message: MessageNode) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return

        // Optimistic UI update via DB
        viewModelScope.launch(Dispatchers.IO) {
            StatusFlow.reportStatus(SyncState.SYNCING_PROFILE, "Pinning message...")
            val chatNode = repository.getChatSync(chatId)
            if (chatNode != null) {
                repository.updateChat(chatNode.copy(pinnedMessageId = message.messageId))
            }
            
            if (isOnline.value) {
                val success = TelegramApi.pinChatMessage(token, chatId, message.messageId)
                if (success) {
                    StatusFlow.reportStatus(SyncState.SUCCESS, "Message pinned")
                } else {
                    if (!NetState.isOnline.value) {
                        StatusFlow.reportStatus(SyncState.ERROR, "Failed to pin: You are offline.")
                    } else {
                        StatusFlow.reportStatus(SyncState.ERROR, "Failed to pin: No admin rights")
                    }
                    // Rollback
                    if (chatNode != null) {
                        repository.updateChat(chatNode)
                    }
                }
            } else {
                StatusFlow.reportStatus(SyncState.ERROR, "Failed to pin: You are offline.")
                // Rollback
                if (chatNode != null) {
                    repository.updateChat(chatNode)
                }
            }
        }
    }

    fun unpinMessage(message: MessageNode) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return

        // Optimistic UI update via DB
        viewModelScope.launch(Dispatchers.IO) {
            StatusFlow.reportStatus(SyncState.SYNCING_PROFILE, "Unpinning message...")
            val chatNode = repository.getChatSync(chatId)
            if (chatNode != null) {
                repository.updateChat(chatNode.copy(pinnedMessageId = null))
            }

            if (isOnline.value) {
                val success = TelegramApi.unpinChatMessage(token, chatId, message.messageId)
                if (success) {
                    StatusFlow.reportStatus(SyncState.SUCCESS, "Message unpinned")
                } else {
                    if (!NetState.isOnline.value) {
                        StatusFlow.reportStatus(SyncState.ERROR, "Failed to unpin: You are offline.")
                    } else {
                        StatusFlow.reportStatus(SyncState.ERROR, "Failed to unpin: No Admin rights.")
                    }
                    // Rollback
                    if (chatNode != null) {
                        repository.updateChat(chatNode)
                    }
                }
            } else {
                StatusFlow.reportStatus(SyncState.ERROR, "Failed to unpin: You are offline.")
                // Rollback
                if (chatNode != null) {
                    repository.updateChat(chatNode)
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

        if (editingMessage != null) {
            val msgToEdit = editingMessage!!
            setEditingMessage(null) // clear state
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateMessageText(msgToEdit.messageId, text)
                var success = false
                if (isOnline.value) {
                    success = TelegramApi.editMessageText(token, chatId, msgToEdit.messageId, text)
                }
                if (!success) {
                    AppLog.log(LogCategory.ERROR, "Failed to edit message via API")
                    repository.updateMessageText(msgToEdit.messageId, msgToEdit.text ?: "")
                    StatusFlow.reportStatus(SyncState.ERROR, "Unable to Edit message")
                }
            }
            return
        }

        val replyToId = replyingToMessage?.messageId
        setReplyingToMessage(null) // clear state

        val messageTime = getNextMessageTime()
        val tempMessageId = -messageTime // Avoid conflict with positive Telegram message IDs
        val initialStatus = if (isOnline.value) MessageStatus.SENDING else MessageStatus.QUEUED

        val newMsg = MessageNode(
            messageId = tempMessageId,
            conversationId = chatId,
            senderId = "ME",
            text = text,
            timestamp = messageTime,
            isFromMe = true,
            status = initialStatus,
            replyToMessageId = replyToId
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureConversationExists(chatId)
            repository.insertMessage(newMsg)

            if (isOnline.value) {
                repository.sendTextMessage(token, chatId, text, tempMessageId, replyToMessageId = replyToId)
            }
        }
    }

    fun sendMedia(context: Context, uri: Uri, mediaType: String): Boolean {
        val chatId = prefs.chatId
        
        if (chatId.isBlank()) return false

        val isOnline = NetState.isOnline.value
        val initialStatus = if (isOnline) MessageStatus.SENDING else MessageStatus.QUEUED

        val fileSize = com.mobile.superiorchat.utils.FileUtils.getFileSize(context, uri)
        if (fileSize > 50 * 1024 * 1024) {
            val formattedSize = com.mobile.superiorchat.utils.FileUtils.formatFileSize(fileSize)
            val fileName = com.mobile.superiorchat.utils.FileUtils.getFileName(context, uri)
            errorPopupMessage = "The selected file '$fileName' ($formattedSize) exceeds the 50MB limit.\n\nFiles larger than 50MB are not supported."
            return false
        }

        val messageTime = getNextMessageTime()
        val tempMessageId = -messageTime

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalName = com.mobile.superiorchat.utils.FileUtils.getFileName(context, uri)
                val newMsg = MessageNode(
                    messageId = tempMessageId,
                    conversationId = chatId,
                    senderId = "ME",
                    text = "",
                    timestamp = messageTime,
                    isFromMe = true,
                    mediaType = mediaType,
                    mediaLocalPath = "", // Will update after copy
                    status = MessageStatus.QUEUED,
                    mediaFileName = originalName,
                    mediaFileSize = fileSize
                )
                repository.ensureConversationExists(chatId)
                repository.insertMessage(newMsg)

                val existingFile = LocalDirs.findLocalSourceMedia(context, mediaType, originalName, fileSize)
                val localFile = existingFile ?: com.mobile.superiorchat.utils.FileUtils.copyUriToLocalFile(context, uri, mediaType, tempMessageId)
                
                if (localFile == null) {
                    repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                    return@launch
                }
                
                // Update the message with the actual local path
                repository.insertMessage(newMsg.copy(mediaLocalPath = localFile.absolutePath))

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

    fun sendMediaBatch(context: Context, items: List<Pair<Uri, String>>): Boolean {
        val chatId = prefs.chatId
        if (chatId.isBlank()) return false

        val validItems = mutableListOf<Triple<Long, Uri, String>>()
        for ((uri, mediaType) in items) {
            val fileSize = com.mobile.superiorchat.utils.FileUtils.getFileSize(context, uri)
            if (fileSize > 50 * 1024 * 1024) {
                val formattedSize = com.mobile.superiorchat.utils.FileUtils.formatFileSize(fileSize)
                val fileName = com.mobile.superiorchat.utils.FileUtils.getFileName(context, uri)
                errorPopupMessage = "The selected file '$fileName' ($formattedSize) exceeds the 50MB limit.\n\nFiles larger than 50MB are not supported."
                return false
            }
            val messageTime = getNextMessageTime()
            validItems.add(Triple(-messageTime, uri, mediaType))
        }

        viewModelScope.launch(Dispatchers.IO) {
            val isOnline = NetState.isOnline.value
            val initialNodes = mutableListOf<MessageNode>()
            
            // First pass: Insert all into UI immediately
            repository.ensureConversationExists(chatId)
            for ((tempMessageId, uri, mediaType) in validItems) {
                val originalName = com.mobile.superiorchat.utils.FileUtils.getFileName(context, uri)
                val newMsg = MessageNode(
                    messageId = tempMessageId,
                    conversationId = chatId,
                    senderId = "ME",
                    text = "",
                    timestamp = -tempMessageId,
                    isFromMe = true,
                    mediaType = mediaType,
                    mediaLocalPath = "",
                    status = MessageStatus.QUEUED,
                    mediaFileName = originalName,
                    mediaFileSize = com.mobile.superiorchat.utils.FileUtils.getFileSize(context, uri)
                )
                initialNodes.add(newMsg)
                repository.insertMessage(newMsg)
            }

            // Second pass: Sequentially copy and enqueue uploads
            for (i in validItems.indices) {
                val (tempMessageId, uri, mediaType) = validItems[i]
                val newMsg = initialNodes[i]
                try {
                    val originalName = com.mobile.superiorchat.utils.FileUtils.getFileName(context, uri)
                    val fileSize = com.mobile.superiorchat.utils.FileUtils.getFileSize(context, uri)
                    val existingFile = LocalDirs.findLocalSourceMedia(context, mediaType, originalName, fileSize)
                    val localFile = existingFile ?: com.mobile.superiorchat.utils.FileUtils.copyUriToLocalFile(context, uri, mediaType, tempMessageId)
                    
                    if (localFile == null) {
                        repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                        continue
                    }
                    
                    repository.insertMessage(newMsg.copy(mediaLocalPath = localFile.absolutePath))

                    if (!localFile.exists()) {
                        repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                        continue
                    }

                    if (isOnline) {
                        repository.updateMessageStatus(tempMessageId, MessageStatus.SENDING)
                        MediaSync.enqueueUpload(context, tempMessageId, localFile.absolutePath, mediaType)
                    }
                    
                    kotlinx.coroutines.delay(100) // Ensure enqueue sequence order
                } catch (e: Exception) {
                    AppLog.log(LogCategory.ERROR, "Failed to send media batch item: ${e.message}")
                    repository.updateMessageStatus(tempMessageId, MessageStatus.FAILED)
                }
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
                repository.sendTextMessage(token, chatId, message.text ?: "", message.messageId)
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
            audioRecorder = com.mobile.superiorchat.media.AudioRecorder(context)
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
            recentImages = repository.getRecentImages(context)
        }
    }

    fun loadAllLocalMedia(context: Context) {
        if (allLocalMedia != null) return // Already loaded
        viewModelScope.launch(Dispatchers.IO) {
            allLocalMedia = repository.getAllLocalMedia(context)
        }
    }

    fun loadRecentFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            recentFiles = repository.getRecentFiles(context)
        }
    }

    fun openDirectory(context: Context, directory: File) {
        currentExplorerDirectory = directory
        viewModelScope.launch(Dispatchers.IO) {
            explorerFilesList = repository.getFilesInDirectory(context, directory)
        }
    }

    fun deleteMessage(message: MessageNode) {
        val chatId = prefs.chatId
        val token = prefs.botToken
        if (chatId.isBlank() || token.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(message.messageId)
            var success = false
            if (NetState.isOnline.value) {
                success = TelegramApi.deleteMessage(token, chatId, message.messageId)
            }
            if (!success) {
                AppLog.log(LogCategory.ERROR, "Failed to delete message via API")
                repository.insertMessage(message)
                StatusFlow.reportStatus(SyncState.ERROR, "Unable Delete for everyone")
            }
        }
    }
}


