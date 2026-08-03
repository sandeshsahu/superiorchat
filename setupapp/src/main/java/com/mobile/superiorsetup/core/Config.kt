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

    var adminBotToken: String
        get() = prefs?.getString("admin_bot_token", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_bot_token", value)?.apply() }

    var adminChatId: String
        get() = prefs?.getString("admin_chat_id", "") ?: ""
        set(value) { prefs?.edit()?.putString("admin_chat_id", value)?.apply() }

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
}
