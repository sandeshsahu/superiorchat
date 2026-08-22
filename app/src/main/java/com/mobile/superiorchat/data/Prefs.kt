package com.mobile.superiorchat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Prefs private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: Prefs? = null

        fun getInstance(context: Context): Prefs {
            return instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
        }
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences: SharedPreferences = try {
        createEncryptedPrefs(context, masterKey)
    } catch (e: Exception) {
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("_androidx_security_master_key_")
        } catch (ignored: Exception) {}
        context.deleteSharedPreferences("secret_shared_prefs")
        try {
            createEncryptedPrefs(context, masterKey)
        } catch (fallback: Exception) {
            context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedPrefs(context: Context, masterKey: MasterKey): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private var _botToken: String = sharedPreferences.getString("bot_token", "").orEmpty()
    var botToken: String
        get() = _botToken
        set(value) {
            if (_botToken != value) {
                _botToken = value
                _lastUpdateId = 0L // Reset last update ID if bot token changes
                sharedPreferences.edit()
                    .putString("bot_token", value)
                    .putLong("last_update_id", 0L)
                    .apply()
            }
        }

    private var _webrtcBaseUrl: String = sharedPreferences.getString(
        "webrtc_base_url", 
        null
    ) ?: context.resources.getStringArray(com.mobile.superiorchat.R.array.webrtc_fallback_urls).firstOrNull() ?: ""
    var webrtcBaseUrl: String
        get() = _webrtcBaseUrl
        set(value) {
            if (_webrtcBaseUrl != value) {
                _webrtcBaseUrl = value
                sharedPreferences.edit().putString("webrtc_base_url", value).apply()
            }
        }

    private var _chatId: String = sharedPreferences.getString("chat_id", "").orEmpty()
    var chatId: String
        get() = _chatId
        set(value) {
            if (_chatId != value) {
                _chatId = value
                sharedPreferences.edit().putString("chat_id", value).apply()
            }
        }

    val activeChatId: String
        get() = if (isPeerLinkEnabled && isAdminModeEnabled && peerLinkGroupChatId.isNotBlank()) peerLinkGroupChatId else chatId

    private var _lastUpdateId: Long = sharedPreferences.getLong("last_update_id", 0L)
    var lastUpdateId: Long
        get() = _lastUpdateId
        set(value) {
            if (_lastUpdateId != value) {
                _lastUpdateId = value
                sharedPreferences.edit().putLong("last_update_id", value).apply()
            }
        }

    private var _isAutoDownloadMediaEnabled: Boolean = sharedPreferences.getBoolean("auto_download_media", false)
    var isAutoDownloadMediaEnabled: Boolean
        get() = _isAutoDownloadMediaEnabled
        set(value) {
            if (_isAutoDownloadMediaEnabled != value) {
                _isAutoDownloadMediaEnabled = value
                sharedPreferences.edit().putBoolean("auto_download_media", value).apply()
            }
        }

    private var _isTileAccessEnabled: Boolean = sharedPreferences.getBoolean("tile_access_enabled", true)
    var isTileAccessEnabled: Boolean
        get() = _isTileAccessEnabled
        set(value) {
            if (_isTileAccessEnabled != value) {
                _isTileAccessEnabled = value
                sharedPreferences.edit().putBoolean("tile_access_enabled", value).apply()
            }
        }

    private var _customDialerCode: String = sharedPreferences.getString("custom_dialer_code", "").orEmpty()
    var customDialerCode: String
        get() = _customDialerCode
        set(value) {
            if (_customDialerCode != value) {
                _customDialerCode = value
                sharedPreferences.edit().putString("custom_dialer_code", value).apply()
            }
        }
        
    private var _isScreenSecurityEnabled: Boolean = sharedPreferences.getBoolean("screen_security_enabled", true)
    var isScreenSecurityEnabled: Boolean
        get() = _isScreenSecurityEnabled
        set(value) {
            if (_isScreenSecurityEnabled != value) {
                _isScreenSecurityEnabled = value
                sharedPreferences.edit().putBoolean("screen_security_enabled", value).apply()
            }
        }

    private var _isNewMessageNotificationEnabled: Boolean = sharedPreferences.getBoolean("new_message_notification_enabled", true)
    var isNewMessageNotificationEnabled: Boolean
        get() = _isNewMessageNotificationEnabled
        set(value) {
            if (_isNewMessageNotificationEnabled != value) {
                _isNewMessageNotificationEnabled = value
                sharedPreferences.edit().putBoolean("new_message_notification_enabled", value).apply()
            }
        }

    private var _isAppNotificationsEnabled: Boolean = sharedPreferences.getBoolean("app_notifications_enabled", true)
    var isAppNotificationsEnabled: Boolean
        get() = _isAppNotificationsEnabled
        set(value) {
            if (_isAppNotificationsEnabled != value) {
                _isAppNotificationsEnabled = value
                sharedPreferences.edit().putBoolean("app_notifications_enabled", value).apply()
            }
        }

    private var _profileEditRateLimitExpiry: Long = sharedPreferences.getLong("profile_edit_rate_limit_expiry", 0L)
    var profileEditRateLimitExpiry: Long
        get() = _profileEditRateLimitExpiry
        set(value) {
            if (_profileEditRateLimitExpiry != value) {
                _profileEditRateLimitExpiry = value
                sharedPreferences.edit().putLong("profile_edit_rate_limit_expiry", value).apply()
            }
        }

    private var _customAccessWord: String = sharedPreferences.getString("custom_access_word", "").orEmpty()
    var customAccessWord: String
        get() = _customAccessWord
        set(value) {
            if (_customAccessWord != value) {
                _customAccessWord = value
                sharedPreferences.edit().putString("custom_access_word", value).apply()
            }
        }

    private var _isAppLockEnabled: Boolean = sharedPreferences.getBoolean("is_app_lock_enabled", false)
    var isAppLockEnabled: Boolean
        get() = _isAppLockEnabled
        set(value) {
            if (_isAppLockEnabled != value) {
                _isAppLockEnabled = value
                sharedPreferences.edit().putBoolean("is_app_lock_enabled", value).apply()
            }
        }

    private var _isFakeCrashEnabled: Boolean = sharedPreferences.getBoolean("is_fake_crash_enabled", false)
    var isFakeCrashEnabled: Boolean
        get() = _isFakeCrashEnabled
        set(value) {
            if (_isFakeCrashEnabled != value) {
                _isFakeCrashEnabled = value
                sharedPreferences.edit().putBoolean("is_fake_crash_enabled", value).apply()
            }
        }

    private var _appLockPin: String = sharedPreferences.getString("app_lock_pin", "").orEmpty()
    var appLockPin: String
        get() = _appLockPin
        set(value) {
            if (_appLockPin != value) {
                _appLockPin = value
                sharedPreferences.edit().putString("app_lock_pin", value).apply()
            }
        }

    private var _appLockPinLength: Int = sharedPreferences.getInt("app_lock_pin_length", 6)
    var appLockPinLength: Int
        get() = _appLockPinLength
        set(value) {
            if (_appLockPinLength != value) {
                _appLockPinLength = value
                sharedPreferences.edit().putInt("app_lock_pin_length", value).apply()
            }
        }

    val isConfigured: Boolean
        get() = botToken.isNotEmpty() && activeChatId.isNotEmpty()

    private var _vaultPaths: Set<String> = sharedPreferences.getStringSet("vault_paths", emptySet<String>()) ?: emptySet()
    var vaultPaths: Set<String>
        get() = _vaultPaths
        set(value) {
            _vaultPaths = value
            sharedPreferences.edit().putStringSet("vault_paths", value).apply()
        }

    private var _appTheme: String = sharedPreferences.getString("app_theme", "LAVENDER") ?: "LAVENDER"
    var appTheme: String
        get() = _appTheme
        set(value) {
            if (_appTheme != value) {
                _appTheme = value
                sharedPreferences.edit().putString("app_theme", value).apply()
            }
        }

    private var _isPeerLinkEnabled: Boolean = sharedPreferences.getBoolean("is_peerlink_enabled", false)
    var isPeerLinkEnabled: Boolean
        get() = _isPeerLinkEnabled
        set(value) {
            if (_isPeerLinkEnabled != value) {
                _isPeerLinkEnabled = value
                sharedPreferences.edit().putBoolean("is_peerlink_enabled", value).apply()
            }
        }

    private var _peerLinkGroupChatId: String = sharedPreferences.getString("peerlink_group_chat_id", "").orEmpty()
    var peerLinkGroupChatId: String
        get() = _peerLinkGroupChatId
        set(value) {
            if (_peerLinkGroupChatId != value) {
                _peerLinkGroupChatId = value
                sharedPreferences.edit().putString("peerlink_group_chat_id", value).apply()
            }
        }

    private var _peerLinkPartnerBotUsername: String = sharedPreferences.getString("peerlink_partner_bot_username", "").orEmpty()
    var peerLinkPartnerBotUsername: String
        get() = _peerLinkPartnerBotUsername
        set(value) {
            if (_peerLinkPartnerBotUsername != value) {
                _peerLinkPartnerBotUsername = value
                sharedPreferences.edit().putString("peerlink_partner_bot_username", value).apply()
            }
        }

    private var _isPeerLinkLocked: Boolean = sharedPreferences.getBoolean("is_peerlink_locked", true)
    var isPeerLinkLocked: Boolean
        get() = _isPeerLinkLocked
        set(value) {
            if (_isPeerLinkLocked != value) {
                _isPeerLinkLocked = value
                sharedPreferences.edit().putBoolean("is_peerlink_locked", value).apply()
            }
        }

    private var _isAdminModeEnabled: Boolean = sharedPreferences.getBoolean("is_admin_mode_enabled", false)
    var isAdminModeEnabled: Boolean
        get() = _isAdminModeEnabled
        set(value) {
            if (_isAdminModeEnabled != value) {
                _isAdminModeEnabled = value
                sharedPreferences.edit().putBoolean("is_admin_mode_enabled", value).apply()
            }
        }

    // Tracks whether the user has agreed to Terms & Conditions.
    // Stored in EncryptedSharedPreferences — automatically resets if user clears app data,
    // which is intentional: T&C must be shown again on fresh install or data wipe.
    private var _hasAgreedToTerms: Boolean = sharedPreferences.getBoolean("has_agreed_to_terms", false)
    var hasAgreedToTerms: Boolean
        get() = _hasAgreedToTerms
        set(value) {
            if (_hasAgreedToTerms != value) {
                _hasAgreedToTerms = value
                sharedPreferences.edit().putBoolean("has_agreed_to_terms", value).apply()
            }
        }
}
