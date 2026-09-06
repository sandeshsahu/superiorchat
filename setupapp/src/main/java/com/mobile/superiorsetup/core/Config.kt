package com.mobile.superiorsetup.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Persistent encrypted store for setup app
object Config {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                "setup_secret_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    var botToken: String
        get() = prefs?.getString("bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("bot_token", value)?.apply() }

    var chatId: String
        get() = prefs?.getString("chat_id", "") ?: ""
        set(value) { prefs?.edit()?.putString("chat_id", value)?.apply() }

    // App-to-App Flow (Explicit Naming)
    var adminAppToAppMyBotToken: String
        get() = prefs?.getString("admin_app_to_app_my_bot_token", "") ?: prefs?.getString("admin_bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_my_bot_token", value)?.apply() }

    var adminAppToAppPartnerBotToken: String
        get() = prefs?.getString("admin_app_to_app_partner_bot_token", "") ?: prefs?.getString("admin_partner_bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_partner_bot_token", value)?.apply() }

    var adminAppToAppGroupId: String
        get() = prefs?.getString("admin_app_to_app_group_id", "") ?: prefs?.getString("admin_chat_id", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_group_id", value)?.apply() }

    var adminAppToAppGroupTitle: String
        get() = prefs?.getString("admin_app_to_app_group_title", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_group_title", value)?.apply() }

    var adminAppToAppMyBotUsername: String
        get() = prefs?.getString("admin_app_to_app_my_bot_username", "") ?: prefs?.getString("admin_my_bot_username", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_my_bot_username", value)?.apply() }

    var adminAppToAppPartnerBotUsername: String
        get() = prefs?.getString("admin_app_to_app_partner_bot_username", "") ?: prefs?.getString("admin_partner_bot_username", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_app_to_app_partner_bot_username", value)?.apply() }

    // Legacy aliases for backward-compatibility
    var adminBotToken: String
        get() = adminAppToAppMyBotToken
        set(value) { adminAppToAppMyBotToken = value }

    var adminChatId: String
        get() = adminAppToAppGroupId
        set(value) { adminAppToAppGroupId = value }

    var adminAutoDownloadMedia: Boolean
        get() = prefs?.getBoolean("admin_auto_download_media", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_auto_download_media", value)?.apply() }

    var adminNewMessageNotification: Boolean
        get() = prefs?.getBoolean("admin_new_message_notification", true) ?: true
        set(value) { prefs?.edit()?.putBoolean("admin_new_message_notification", value)?.apply() }

    var adminBlockScreenshots: Boolean
        get() = prefs?.getBoolean("admin_block_screenshots", true) ?: true
        set(value) { prefs?.edit()?.putBoolean("admin_block_screenshots", value)?.apply() }

    var adminCallServer: String
        get() = prefs?.getString("admin_call_server", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_call_server", value)?.apply() }

    var adminLastGeneratedState: String
        get() = prefs?.getString("admin_last_generated_state", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_last_generated_state", value)?.apply() }

    var adminLastGeneratedPin: String
        get() = prefs?.getString("admin_last_generated_pin", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_last_generated_pin", value)?.apply() }

    var adminLastEncryptedPayload: String
        get() = prefs?.getString("admin_last_encrypted_payload", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_last_encrypted_payload", value)?.apply() }

    var adminRequirePin: Boolean
        get() = prefs?.getBoolean("admin_require_pin", true) ?: true
        set(value) { prefs?.edit()?.putBoolean("admin_require_pin", value)?.apply() }
        
    var adminTheme: String
        get() = prefs?.getString("admin_theme", "LAVENDER") ?: "LAVENDER"
        set(value) { prefs?.edit()?.putString("admin_theme", value)?.apply() }

    var adminChatMode: String
        get() = prefs?.getString("admin_chat_mode", "TELEGRAM") ?: "TELEGRAM"
        set(value) { prefs?.edit()?.putString("admin_chat_mode", value)?.apply() }

    var adminPartnerBotUsername: String
        get() = adminAppToAppPartnerBotUsername
        set(value) { adminAppToAppPartnerBotUsername = value }

    var adminMyBotUsername: String
        get() = adminAppToAppMyBotUsername
        set(value) { adminAppToAppMyBotUsername = value }

    var adminPartnerBotToken: String
        get() = adminAppToAppPartnerBotToken
        set(value) { adminAppToAppPartnerBotToken = value }

    var adminIsPeerLinkEnabled: Boolean
        get() = prefs?.getBoolean("admin_is_peer_link_enabled", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_is_peer_link_enabled", value)?.apply() }

    var adminIsReadOnly: Boolean
        get() = prefs?.getBoolean("admin_is_read_only", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_is_read_only", value)?.apply() }

    var adminCustomAccessWord: String
        get() = prefs?.getString("admin_custom_access_word", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_custom_access_word", value)?.apply() }

    var adminCustomDialerCode: String
        get() = prefs?.getString("admin_custom_dialer_code", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_custom_dialer_code", value)?.apply() }

    var adminCallNotifications: Boolean
        get() = prefs?.getBoolean("admin_call_notifications", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_call_notifications", value)?.apply() }

    var adminLastAdminEncryptedPayload: String
        get() = prefs?.getString("admin_last_admin_encrypted_payload", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_last_admin_encrypted_payload", value)?.apply() }

    // Telegram 1-on-1 Flow (Explicit Naming)
    var adminTelegramBotToken: String
        get() = prefs?.getString("admin_telegram_bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_telegram_bot_token", value)?.apply() }

    var adminTelegramChatId: String
        get() = prefs?.getString("admin_telegram_chat_id", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_telegram_chat_id", value)?.apply() }

    var adminTelegramChatName: String
        get() = prefs?.getString("admin_telegram_chat_name", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_telegram_chat_name", value)?.apply() }

    var adminTelegramBotUsername: String
        get() = prefs?.getString("admin_telegram_bot_username", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_telegram_bot_username", value)?.apply() }

    var adminHardLockAdmin: Boolean
        get() = prefs?.getBoolean("admin_hard_lock_admin", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_hard_lock_admin", value)?.apply() }

    var adminHardLockPartner: Boolean
        get() = prefs?.getBoolean("admin_hard_lock_partner", true) ?: true
        set(value) { prefs?.edit()?.putBoolean("admin_hard_lock_partner", value)?.apply() }

    var adminIsAdminModeEnabled: Boolean
        get() = prefs?.getBoolean("admin_is_admin_mode_enabled", false) ?: false
        set(value) { prefs?.edit()?.putBoolean("admin_is_admin_mode_enabled", value)?.apply() }
}

