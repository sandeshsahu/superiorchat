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

    private inner class StringPref(val key: String, val defaultValue: String = "") {
        private var cachedValue: String? = null
        operator fun getValue(thisRef: Any?, property: Any?): String {
            return cachedValue ?: sharedPreferences.getString(key, defaultValue).orEmpty().also { cachedValue = it }
        }
        operator fun setValue(thisRef: Any?, property: Any?, value: String) {
            cachedValue = value
            sharedPreferences.edit().putString(key, value).apply()
        }
    }


    var botToken by StringPref("bot_token")
    var chatId by StringPref("chat_id")

    val isConfigured: Boolean
        get() = botToken.isNotEmpty() && chatId.isNotEmpty()
}
