package com.mobile.superiorchat.ui

import com.mobile.superiorchat.BuildConfig
import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.app.AppOpsManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
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
import com.mobile.superiorchat.theme.AppTheme
import com.mobile.superiorchat.theme.applyTheme
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
    val hasManageStorage: Boolean = false,
    val hasPipPermission: Boolean = false
) {
    val allPermissionsGranted: Boolean
        get() = hasPostNotifs && hasIgnoreBattery && hasCamera && hasMicrophone && mediaAccessLevel == MediaAccessLevel.FULL && hasInstallPackages && hasManageStorage
}

sealed class GlobalDialogState {
    data class PermissionPermanentlyDenied(val intent: Intent) : GlobalDialogState()
    data class NotificationPermanentlyDenied(val onGoToSettings: () -> Unit, val onDismiss: () -> Unit) : GlobalDialogState()
    data class BatteryOptimizationRequired(val onRetry: () -> Unit, val onDismiss: () -> Unit) : GlobalDialogState()
    data class PartialMediaAccessPermanentlyDenied(val onContinue: () -> Unit, val onGoToSettings: () -> Unit) : GlobalDialogState()
    data class ManageStorageRequired(val intent: Intent) : GlobalDialogState()
    data class PartialMediaAccess(val onContinue: () -> Unit, val onUpgrade: () -> Unit) : GlobalDialogState()
    data class CameraPermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class MicrophonePermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class StoragePermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
    data class CallPermissionRationale(val onConfirm: () -> Unit) : GlobalDialogState()
}

enum class UnlockResult { SUCCESS, INVALID }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var activeGlobalDialog by mutableStateOf<GlobalDialogState?>(null)


    private val prefs = AppGraph.prefs

    val isNetworkAvailable: StateFlow<Boolean> = NetState.isOnline

    private var prefListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    init {
        try {
            applyTheme(AppTheme.valueOf(prefs.appTheme))
        } catch (e: Exception) {
            applyTheme(AppTheme.LAVENDER)
        }
        
        prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "bot_token" -> botToken = prefs.botToken
                "chat_id" -> chatId = prefs.chatId
                "webrtc_base_url" -> webrtcBaseUrl = prefs.webrtcBaseUrl
                "peerlink_group_chat_id" -> peerLinkGroupChatId = prefs.peerLinkGroupChatId
                "peerlink_partner_bot_username" -> peerLinkPartnerBotUsername = prefs.peerLinkPartnerBotUsername
                "is_peerlink_enabled" -> isPeerLinkEnabled = prefs.isPeerLinkEnabled
                "is_admin_mode_enabled" -> isAdminModeEnabled = prefs.isAdminModeEnabled
                "is_peerlink_locked" -> isPeerLinkLocked = prefs.isPeerLinkLocked
                "is_peerlink_hard_locked" -> isPeerLinkHardLocked = prefs.isPeerLinkHardLocked
                "is_exclude_from_recents_enabled" -> isExcludeFromRecentsEnabled = prefs.isExcludeFromRecentsEnabled
                "is_background_calls_enabled" -> isBackgroundCallsEnabled = prefs.isBackgroundCallsEnabled
                "is_call_ringing_enabled" -> isCallRingingEnabled = prefs.isCallRingingEnabled
                "is_anime_character_enabled" -> isAnimeCharacterEnabled = prefs.isAnimeCharacterEnabled
                "auto_download_media" -> autoDownloadMedia = prefs.isAutoDownloadMediaEnabled
                "screen_security_enabled" -> isScreenSecurityEnabled = prefs.isScreenSecurityEnabled
                "new_message_notification_enabled" -> newMessageNotificationEnabled = prefs.isNewMessageNotificationEnabled
                "custom_access_word" -> customAccessWord = prefs.customAccessWord
                "is_default_access_word_enabled" -> isDefaultAccessWordEnabled = prefs.isDefaultAccessWordEnabled
                "custom_dialer_code" -> customDialerCode = prefs.customDialerCode
                "is_default_dialer_code_enabled" -> isDefaultDialerCodeEnabled = prefs.isDefaultDialerCodeEnabled
                "app_theme" -> {
                    try {
                        val theme = AppTheme.valueOf(prefs.appTheme)
                        appTheme = theme
                        applyTheme(theme)
                    } catch (e: Exception) {}
                }
            }
            checkTelegramConnection()
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
    var webrtcBaseUrl by mutableStateOf(prefs.webrtcBaseUrl)
    
    var appTheme by mutableStateOf(
        try { AppTheme.valueOf(prefs.appTheme) } catch(e: Exception) { AppTheme.LAVENDER }
    )
        private set
        
    fun updateAppTheme(theme: AppTheme) {
        prefs.appTheme = theme.name
        appTheme = theme
        applyTheme(theme)
    }

    val hasCredentials: Boolean
        get() {
            val validToken = botToken.trim().matches(Regex("^[0-9]+:[a-zA-Z0-9_-]+$"))
            val targetChatId = if (isAdminModeEnabled) peerLinkGroupChatId else chatId
            val validChatId = targetChatId.trim().matches(Regex("^-?[0-9]+$"))
            return validToken && validChatId
        }

    val tokenStatusText: String
        get() = when {
            !hasCredentials -> "Invalid"
            !isNetworkAvailable.value -> "Offline"
            !isTelegramApiReachable.value -> "Invalid"
            else -> "Online"
        }

    // -- App Lock State --
    var isDuressModeActive by mutableStateOf(false)
        private set

    var isFakeCrashBypassed by mutableStateOf(false)
        private set

    private val _isAppUnlocked = MutableStateFlow(!(prefs.isAppLockEnabled || prefs.isFakeCrashEnabled))
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    fun verifyPin(pin: String): Boolean {
        if (prefs.appLockPin.isEmpty()) return true
        val hashedInput = com.mobile.superiorchat.utils.Security.hashSHA256(pin)
        return hashedInput == prefs.appLockPin
    }

    fun unlockApp(pin: String): UnlockResult {
        if (pin.startsWith("1234")) {
            isDuressModeActive = true
            _isAppUnlocked.value = true
            com.mobile.superiorchat.core.AppGraph.isChatInForeground = true
            return UnlockResult.SUCCESS
        }
        val hashedInput = com.mobile.superiorchat.utils.Security.hashSHA256(pin)
        return if (hashedInput == prefs.appLockPin || prefs.appLockPin.isEmpty()) {
            isDuressModeActive = false
            _isAppUnlocked.value = true
            com.mobile.superiorchat.core.AppGraph.isChatInForeground = true
            UnlockResult.SUCCESS
        } else {
            UnlockResult.INVALID
        }
    }

    fun bypassFakeCrash() {
        isFakeCrashBypassed = true
        if (!prefs.isAppLockEnabled) {
            _isAppUnlocked.value = true
            com.mobile.superiorchat.core.AppGraph.isChatInForeground = true
        }
    }

    fun lockApp() {
        com.mobile.superiorchat.core.AppGraph.isChatInForeground = false
        if (prefs.isAppLockEnabled || prefs.isFakeCrashEnabled) {
            _isAppUnlocked.value = false
            isFakeCrashBypassed = false
            isDuressModeActive = false
        }
    }


    var isAppLockEnabled by mutableStateOf(prefs.isAppLockEnabled)
        private set
    fun toggleAppLock(enabled: Boolean) {
        prefs.isAppLockEnabled = enabled
        isAppLockEnabled = enabled
    }

    var isFakeCrashEnabled by mutableStateOf(prefs.isFakeCrashEnabled)
        private set
    fun toggleFakeCrash(enabled: Boolean) {
        prefs.isFakeCrashEnabled = enabled
        isFakeCrashEnabled = enabled
    }

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

    var isDefaultAccessWordEnabled by mutableStateOf(prefs.isDefaultAccessWordEnabled)
        private set

    fun toggleDefaultAccessWord(enabled: Boolean) {
        prefs.isDefaultAccessWordEnabled = enabled
        isDefaultAccessWordEnabled = enabled
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            if (enabled) "Default Keyword Fallback Enabled" else "Default Keyword Fallback Disabled"
        )
    }
        
    var customDialerCode by mutableStateOf(prefs.customDialerCode)
        private set

    var isDefaultDialerCodeEnabled by mutableStateOf(prefs.isDefaultDialerCodeEnabled)
        private set

    fun toggleDefaultDialerCode(enabled: Boolean) {
        prefs.isDefaultDialerCodeEnabled = enabled
        isDefaultDialerCodeEnabled = enabled
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            if (enabled) "Default Code Fallback Enabled" else "Default Code Fallback Disabled"
        )
    }
        
    var showAppLevelQrScanner by mutableStateOf(false)

    fun updateCustomAccessWord(word: String) {
        prefs.customAccessWord = word
        customAccessWord = word
        if (word.isBlank()) {
            isDefaultAccessWordEnabled = true
        }
        com.mobile.superiorchat.core.StatusFlow.reportStatus(
            com.mobile.superiorchat.core.SyncState.SUCCESS, 
            "Custom Word Saved"
        )
    }
    
    fun updateCustomDialerCode(code: String) {
        prefs.customDialerCode = code
        customDialerCode = code
        if (code.isBlank()) {
            isDefaultDialerCodeEnabled = true
        }
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

    // -- PeerLink State --
    var isPeerLinkEnabled by mutableStateOf(prefs.isPeerLinkEnabled)
        private set

    fun togglePeerLink(enabled: Boolean) {
        prefs.isPeerLinkEnabled = enabled
        isPeerLinkEnabled = enabled
        if (!enabled) {
            prefs.peerLinkPartnerBotUsername = ""
            peerLinkPartnerBotUsername = ""
            if (isAdminModeEnabled) {
                prefs.isAdminModeEnabled = false
                isAdminModeEnabled = false
            }
        }
    }

    var peerLinkGroupChatId by mutableStateOf(prefs.peerLinkGroupChatId)
        private set

    fun updatePeerLinkGroupId(id: String) {
        prefs.peerLinkGroupChatId = id
        peerLinkGroupChatId = id
    }

    var peerLinkPartnerBotUsername by mutableStateOf(prefs.peerLinkPartnerBotUsername)
        private set

    fun updatePeerLinkPartnerUsername(username: String) {
        prefs.peerLinkPartnerBotUsername = username
        peerLinkPartnerBotUsername = username
    }

    var isPeerLinkLocked by mutableStateOf(prefs.isPeerLinkLocked)
        private set

    var isPeerLinkHardLocked by mutableStateOf(prefs.isPeerLinkHardLocked)
        private set

    fun togglePeerLinkLocked(locked: Boolean) {
        if (isPeerLinkHardLocked) return
        prefs.isPeerLinkLocked = locked
        isPeerLinkLocked = locked
        if (locked) {
            // When locking, we should probably not turn off admin mode automatically?
            // User requested no complex logic here, it just locks UI interaction.
        }
    }

    var isAdminModeEnabled by mutableStateOf(prefs.isAdminModeEnabled)
        private set

    fun toggleAdminMode(enabled: Boolean) {
        prefs.isAdminModeEnabled = enabled
        isAdminModeEnabled = enabled
        if (!enabled) {
            prefs.peerLinkPartnerBotUsername = ""
            peerLinkPartnerBotUsername = ""
            clearCredentials()
        }
    }

    var isExcludeFromRecentsEnabled by mutableStateOf(prefs.isExcludeFromRecentsEnabled)
        private set

    fun toggleExcludeFromRecents(enabled: Boolean) {
        prefs.isExcludeFromRecentsEnabled = enabled
        isExcludeFromRecentsEnabled = enabled
    }

    var isBackgroundCallsEnabled by mutableStateOf(prefs.isBackgroundCallsEnabled)
        private set

    fun toggleBackgroundCalls(enabled: Boolean) {
        prefs.isBackgroundCallsEnabled = enabled
        isBackgroundCallsEnabled = enabled
    }

    var isCallRingingEnabled by mutableStateOf(prefs.isCallRingingEnabled)
        private set

    fun toggleCallRinging(enabled: Boolean) {
        prefs.isCallRingingEnabled = enabled
        isCallRingingEnabled = enabled
    }

    var isAnimeCharacterEnabled by mutableStateOf(prefs.isAnimeCharacterEnabled)
        private set

    fun toggleAnimeCharacter(enabled: Boolean) {
        prefs.isAnimeCharacterEnabled = enabled
        isAnimeCharacterEnabled = enabled
    }

    fun applyQrConfig(data: com.mobile.superiorchat.utils.QrConfigData) {
        val token = data.token.trim()
        val chat = data.chatId.trim()
        botToken = token
        chatId = chat
        prefs.botToken = token
        prefs.chatId = chat
        prefs.lastUpdateId = 0L

        if (data.role == "ADMIN" || data.isAdminModeEnabled == true) {
            // Admin QR: enable Route Messages and I Will Chat Here
            prefs.isPeerLinkEnabled = true
            isPeerLinkEnabled = true
            prefs.isAdminModeEnabled = true
            isAdminModeEnabled = true
            prefs.peerLinkGroupChatId = chat
            peerLinkGroupChatId = chat
            val partner = data.partnerUsername ?: ""
            prefs.peerLinkPartnerBotUsername = partner
            peerLinkPartnerBotUsername = partner
        } else {
            // Client / Partner QR
            val peerLink = data.isPeerLinkEnabled ?: false
            prefs.isPeerLinkEnabled = peerLink
            isPeerLinkEnabled = peerLink
            prefs.isAdminModeEnabled = false
            isAdminModeEnabled = false
            val partner = if (peerLink) (data.partnerUsername ?: "") else ""
            prefs.peerLinkPartnerBotUsername = partner
            peerLinkPartnerBotUsername = partner
            prefs.peerLinkGroupChatId = ""
            peerLinkGroupChatId = ""
        }

        data.isHardLocked?.let { hardLocked ->
            prefs.isPeerLinkHardLocked = hardLocked
            isPeerLinkHardLocked = hardLocked
            prefs.isPeerLinkLocked = true
            isPeerLinkLocked = true
        }

        data.autoDownloadMedia?.let { toggleAutoDownloadMedia(it) }
        data.screenSecurity?.let { toggleScreenSecurity(it) }
        data.newMessageNotification?.let { toggleNewMessageNotification(it) }
        data.callNotifications?.let {
            if (it) {
                prefs.hasPromptedPostNotifs = false
            }
            toggleCallRinging(it)
        }
        data.callServer?.let { 
            webrtcBaseUrl = it
            prefs.webrtcBaseUrl = it
        }
        data.theme?.let {
            try {
                updateAppTheme(AppTheme.valueOf(it))
            } catch (e: Exception) {}
        }

        if (BuildConfig.FLAVOR == "weather") {
            data.customAccessWord?.let {
                if (it.isNotBlank()) updateCustomAccessWord(it)
            }
        } else if (BuildConfig.FLAVOR == "captivePortal" || BuildConfig.FLAVOR == "playSupport") {
            data.customDialerCode?.let {
                if (it.isNotBlank()) updateCustomDialerCode(it)
            }
        }

        saveCredentials()
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
            
            val areNotifsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val hasPostNotifs = areNotifsEnabled && (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )

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

            val hasPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        Process.myUid(),
                        context.packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        Process.myUid(),
                        context.packageName
                    )
                }
                mode == AppOpsManager.MODE_ALLOWED
            } else false

            _permissionStatus.value = PermissionStatus(
                hasPostNotifs = hasPostNotifs,
                hasIgnoreBattery = hasIgnoreBattery,
                isInternetConnected = isInternetConnected,
                hasCamera = hasCamera,
                hasMicrophone = hasMicrophone,
                mediaAccessLevel = mediaAccessLevel,
                hasInstallPackages = hasInstallPackages,
                hasManageStorage = hasManageStorage,
                hasPipPermission = hasPip
            )

            // Auto-disable Background Calls if the user has revoked PiP permission from OS Settings.
            // Detected on every ON_RESUME via AppNav's refreshPermissions() call.
            if (!hasPip && prefs.isBackgroundCallsEnabled) {
                prefs.isBackgroundCallsEnabled = false
                isBackgroundCallsEnabled = false
            }

            // Auto-disable Call Ringing if the user has revoked Notification permission from OS Settings.
            // Only auto-disable if the notification permission prompt has already been completed,
            // preventing fresh installs on Android 13+ from immediately silencing calls before the prompt is answered.
            if (!hasPostNotifs && prefs.hasPromptedPostNotifs && prefs.isCallRingingEnabled) {
                prefs.isCallRingingEnabled = false
                isCallRingingEnabled = false
            }

            if (hasPostNotifs) {
                prefs.hasPromptedPostNotifs = true
                prefs.hasEverGrantedPostNotifs = true
                if (!prefs.isAppNotificationsEnabled) {
                    prefs.isAppNotificationsEnabled = true
                }
            } else if (prefs.hasEverGrantedPostNotifs) {
                // User previously granted notification permission, but revoked it in Android OS directly!
                // Treat as intentional stealth: update app preference to false so no denial popups appear.
                if (prefs.isAppNotificationsEnabled) {
                    prefs.isAppNotificationsEnabled = false
                }
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
        
        val tokenChanged = prefs.botToken != token
        val chatChanged = prefs.chatId != chat
        if (tokenChanged || chatChanged) {
            prefs.lastUpdateId = 0L
        }

        prefs.botToken = token
        prefs.chatId = chat
        prefs.webrtcBaseUrl = webrtcBaseUrl
        
        botToken = token
        chatId = chat

        checkTelegramConnection()

        if (prefs.isConfigured) {
            ServiceCore.restart(getApplication<Application>())
            com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Saved")
        } else {
            ServiceCore.stop(getApplication<Application>())
            com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Cleared")
        }
    }

    fun clearCredentials() {
        botToken = ""
        chatId = ""
        peerLinkGroupChatId = ""
        peerLinkPartnerBotUsername = ""
        isPeerLinkEnabled = false
        isAdminModeEnabled = false
        isPeerLinkLocked = true
        isPeerLinkHardLocked = false

        prefs.botToken = ""
        prefs.chatId = ""
        prefs.peerLinkGroupChatId = ""
        prefs.peerLinkPartnerBotUsername = ""
        prefs.isPeerLinkEnabled = false
        prefs.isAdminModeEnabled = false
        prefs.isPeerLinkLocked = true
        prefs.isPeerLinkHardLocked = false
        prefs.lastUpdateId = 0L

        checkTelegramConnection()
        ServiceCore.stop(getApplication<Application>())
        com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Credentials Cleared")
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
