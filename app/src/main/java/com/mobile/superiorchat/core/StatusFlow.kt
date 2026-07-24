package com.mobile.superiorchat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class SyncState {
    IDLE,
    SYNCING_PROFILE,
    SYNCING_MESSAGES,
    TRANSFERRING,
    SUCCESS,
    ERROR,
    OFFLINE,
    AUTH_ERROR
}

data class ActiveTransfer(
    val messageId: Long,
    val isUpload: Boolean,
    val mediaType: String,
    val fileName: String?,
    val localPath: String? = null,
    val progressFlow: MutableStateFlow<Float> = MutableStateFlow(0f)
)

object StatusFlow {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    
    private var resetJob: Job? = null

    // Persistent state (like OFFLINE or AUTH_ERROR)
    private var persistentState = SyncState.IDLE
    private var persistentMessage: String? = null

    // Active transfers registry
    private val activeTransfersMap = ConcurrentHashMap<Long, ActiveTransfer>()
    private val _activeTransfers = MutableStateFlow<List<ActiveTransfer>>(emptyList())
    val activeTransfers: StateFlow<List<ActiveTransfer>> = _activeTransfers.asStateFlow()

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    fun reportStatus(state: SyncState, message: String) {
        resetJob?.cancel()
        
        // 1. Update persistent state for permanent modes/errors
        if (state == SyncState.OFFLINE || state == SyncState.AUTH_ERROR) {
            persistentState = state
            persistentMessage = message
            _syncState.value = state
            _syncMessage.value = message
            return
        } else if (state == SyncState.IDLE || state == SyncState.SUCCESS) {
            persistentState = SyncState.IDLE
            persistentMessage = null
        }

        // If currently locked in persistent offline/auth error, ignore temporary updates
        if (persistentState == SyncState.OFFLINE || persistentState == SyncState.AUTH_ERROR) {
            _syncState.value = persistentState
            _syncMessage.value = persistentMessage
            return
        }

        // 2. Set current state and message
        _syncState.value = state
        _syncMessage.value = message
        
        // 3. Auto-reset for temporary statuses (SUCCESS, ERROR)
        if (state == SyncState.SUCCESS || state == SyncState.ERROR) {
            resetJob = scope.launch {
                delay(2000)
                if (activeTransfersMap.isNotEmpty()) {
                    updateActiveTransfersState()
                } else {
                    _syncState.value = persistentState
                    _syncMessage.value = persistentMessage
                }
            }
        }
    }

    fun registerTransfer(messageId: Long, isUpload: Boolean, mediaType: String, fileName: String?, localPath: String? = null): MutableStateFlow<Float> {
        val transfer = activeTransfersMap.getOrPut(messageId) {
            ActiveTransfer(messageId, isUpload, mediaType, fileName, localPath)
        }
        updateActiveTransfersState()
        return transfer.progressFlow
    }

    fun updateProgress(messageId: Long, progress: Float) {
        val transfer = activeTransfersMap[messageId]
        if (transfer != null) {
            transfer.progressFlow.value = progress
            recalculateOverallProgress()
        }
    }

    fun unregisterTransfer(messageId: Long) {
        activeTransfersMap.remove(messageId)
        updateActiveTransfersState()
    }

    private fun recalculateOverallProgress() {
        val list = activeTransfersMap.values.toList()
        if (list.isEmpty()) {
            _overallProgress.value = 0f
        } else {
            val sum = list.sumOf { it.progressFlow.value.toDouble() }
            _overallProgress.value = (sum / list.size).toFloat().coerceIn(0f, 1f)
        }
    }

    private fun updateActiveTransfersState() {
        val list = activeTransfersMap.values.toList()
        _activeTransfers.value = list
        recalculateOverallProgress()

        // Do not overwrite persistent offline or auth errors
        if (persistentState == SyncState.OFFLINE || persistentState == SyncState.AUTH_ERROR) {
            _syncState.value = persistentState
            _syncMessage.value = persistentMessage
            return
        }

        if (list.isNotEmpty()) {
            if (resetJob?.isActive != true) {
                _syncState.value = SyncState.TRANSFERRING
                val uploads = list.count { it.isUpload }
                val downloads = list.size - uploads
                val msg = when {
                    uploads > 0 && downloads > 0 -> "Transferring ${list.size} items..."
                    uploads > 0 -> "Uploading $uploads item${if (uploads > 1) "s" else ""}..."
                    else -> "Downloading $downloads item${if (downloads > 1) "s" else ""}..."
                }
                _syncMessage.value = msg
            }
        } else {
            if (_syncState.value == SyncState.TRANSFERRING || resetJob?.isActive != true) {
                _syncState.value = persistentState
                _syncMessage.value = persistentMessage
            }
        }
    }
}
