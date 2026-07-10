package com.mobile.superiorutils.data

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

    var botToken: String
        get() = sharedPreferences.getString("bot_token", "").orEmpty()
        set(value) {
            val current = sharedPreferences.getString("bot_token", "")
            if (current != value) {
                sharedPreferences.edit()
                    .putString("bot_token", value)
                    .putLong("last_update_id", 0L)
                    .apply()
            }
        }

    var chatId: String
        get() = sharedPreferences.getString("chat_id", "").orEmpty()
        set(value) {
            sharedPreferences.edit().putString("chat_id", value).apply()
        }

    var lastUpdateId: Long
        get() = sharedPreferences.getLong("last_update_id", 0L)
        set(value) {
            sharedPreferences.edit().putLong("last_update_id", value).apply()
        }

    val isConfigured: Boolean
        get() = botToken.isNotEmpty() && chatId.isNotEmpty()
}
