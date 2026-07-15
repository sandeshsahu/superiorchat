package com.mobile.superiorutils.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncState {
    IDLE,
    SYNCING_PROFILE,
    SYNCING_MESSAGES,
    SUCCESS,
    ERROR,
    OFFLINE,
    AUTH_ERROR
}

object StatusFlow {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()
    
    private var resetJob: Job? = null

    fun reportStatus(state: SyncState, message: String) {
        // Cancel any pending reset if a new status comes in
        resetJob?.cancel()
        
        _syncState.value = state
        _syncMessage.value = message
        
        // If it's a terminal state (SUCCESS or ERROR), auto-hide after 2 seconds
        // Exception: Auth errors (AUTH_ERROR) and OFFLINE remain visible permanently until fixed.
        if (state == SyncState.SUCCESS || state == SyncState.ERROR) {
            resetJob = scope.launch {
                delay(2000)
                _syncState.value = SyncState.IDLE
                _syncMessage.value = null
            }
        }
    }
}
