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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MediaAccessLevel { NONE, PARTIAL, FULL }

data class PermissionStatus(
    val hasPostNotifs: Boolean = false,
    val hasIgnoreBattery: Boolean = false,
    val isInternetConnected: Boolean = false,
    val hasCamera: Boolean = false,
    val hasMicrophone: Boolean = false,
    val mediaAccessLevel: MediaAccessLevel = MediaAccessLevel.NONE,
    val hasInstallPackages: Boolean = false,
    val hasManageStorage: Boolean = false
) {
    val allPermissionsGranted: Boolean
        get() = hasPostNotifs && hasIgnoreBattery && hasCamera && hasMicrophone && mediaAccessLevel == MediaAccessLevel.FULL && hasInstallPackages && hasManageStorage
}

sealed class GlobalDialogState {
    data class PermissionPermanentlyDenied(val intent: Intent) : GlobalDialogState()
    data class PartialMediaAccessPermanentlyDenied(val onContinue: () -> Unit, val onGoToSettings: () -> Unit) : GlobalDialogState()
    data class ManageStorageRequired(val intent: Intent) : GlobalDialogState()
    data class PartialMediaAccess(val onContinue: () -> Unit, val onUpgrade: () -> Unit) : GlobalDialogState()
    data class CameraPermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class MicrophonePermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class StoragePermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class CallPermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var activeGlobalDialog by mutableStateOf<GlobalDialogState?>(null)


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

    // -- Preferences State --
    var autoDownloadMedia by mutableStateOf(prefs.isAutoDownloadMediaEnabled)
        private set

    fun toggleAutoDownloadMedia(enabled: Boolean) {
        prefs.isAutoDownloadMediaEnabled = enabled
        autoDownloadMedia = enabled
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            if (enabled) "Auto-Download Enabled" else "Auto-Download Disabled"
        )
    }

    var tileAccessEnabled by mutableStateOf(prefs.isTileAccessEnabled)
        private set

    fun toggleTileAccess(enabled: Boolean) {
        prefs.isTileAccessEnabled = enabled
        tileAccessEnabled = enabled
    }

    var customAccessWord by mutableStateOf(prefs.customAccessWord)
        private set

    fun updateCustomAccessWord(word: String) {
        prefs.customAccessWord = word
        customAccessWord = word
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            "Custom Word Saved"
        )
    }

    var isScreenSecurityEnabled by mutableStateOf(prefs.isScreenSecurityEnabled)
        private set

    fun toggleScreenSecurity(enabled: Boolean) {
        prefs.isScreenSecurityEnabled = enabled
        isScreenSecurityEnabled = enabled
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            if (enabled) "Screenshot blocking Enabled" else "Screenshot blocking Disabled"
        )
    }

    var newMessageNotificationEnabled by mutableStateOf(prefs.isNewMessageNotificationEnabled)
        private set

    fun toggleNewMessageNotification(enabled: Boolean) {
        prefs.isNewMessageNotificationEnabled = enabled
        newMessageNotificationEnabled = enabled
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            if (enabled) "New Message Notifications Enabled" else "New Message Notifications Disabled"
        )
    }

    var appNotificationsEnabled by mutableStateOf(prefs.isAppNotificationsEnabled)
        private set

    fun updateAppNotificationsState(enabled: Boolean) {
        prefs.isAppNotificationsEnabled = enabled
        appNotificationsEnabled = enabled
    }

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

            val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val hasMicrophone = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

            val mediaAccessLevel = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // Android 14+ (API 34+)
                    val images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    val partial = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                    
                    if (images && video) MediaAccessLevel.FULL
                    else if (partial) MediaAccessLevel.PARTIAL
                    else MediaAccessLevel.NONE
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU -> { // Android 13 (API 33)
                    val images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    if (images && video) MediaAccessLevel.FULL else MediaAccessLevel.NONE
                }
                else -> { // Android 12 and below
                    val storage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    if (storage) MediaAccessLevel.FULL else MediaAccessLevel.NONE
                }
            }

            val hasInstallPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }

            val hasManageStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                true
            }

            _permissionStatus.value = PermissionStatus(
                hasPostNotifs = hasPostNotifs,
                hasIgnoreBattery = hasIgnoreBattery,
                isInternetConnected = isInternetConnected,
                hasCamera = hasCamera,
                hasMicrophone = hasMicrophone,
                mediaAccessLevel = mediaAccessLevel,
                hasInstallPackages = hasInstallPackages,
                hasManageStorage = hasManageStorage
            )
            
            // We ONLY want to sync from OS to Prefs if the OS is ENABLED. 
            // If it's disabled, it might just be a fresh install (Android 13+ defaults to denied),
            // so we don't want to blindly overwrite our default `true` to `false`, which would break the startup prompt!
            if (hasPostNotifs && !prefs.isAppNotificationsEnabled) {
                prefs.isAppNotificationsEnabled = true
            }
            appNotificationsEnabled = prefs.isAppNotificationsEnabled
        }
    }

    fun toggleAppNotificationsEnabled(enabled: Boolean) {
        prefs.isAppNotificationsEnabled = enabled
        appNotificationsEnabled = enabled
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
            com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Saved")
        } else {
            com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Cleared")
        }
    }

    fun clearChat(deleteMedia: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppGraph.database.messageDao().clearAllMessages()
                if (deleteMedia) {
                    LocalDirs.getBaseDir(getApplication()).deleteRecursively()
                    com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Chat and media cleared")
                } else {
                    com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Chat history cleared")
                }
                AppLog.log(LogCategory.SYSTEM, "Chat history cleared from local database", LogLevel.DEBUG)
            } catch (e: Exception) {
                com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.ERROR, "Failed to clear chat")
                AppLog.log(LogCategory.SYSTEM, "Error clearing chat history: ${e.message}", LogLevel.ERROR)
            }
        }
    }
}
