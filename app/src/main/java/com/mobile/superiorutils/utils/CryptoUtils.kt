package com.mobile.superiorutils.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

object CryptoUtils {
    private const val KEY_ALIAS = "SuperiorChatIPCKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"

    init {
        generateKeyStoreKeyIfNeeded()
    }

    private fun generateKeyStoreKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
                )
                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
                )
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .build()
                
                keyPairGenerator.initialize(parameterSpec)
                keyPairGenerator.generateKeyPair()
                AppLog.log(LogCategory.SYSTEM, "Generated new RSA Keystore key pair for IPC.")
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to generate Keystore key: ${e.message}", LogLevel.ERROR)
        }
    }

    fun getPublicKeyBase64(): String {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey
            if (publicKey != null) {
                return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to retrieve public key: ${e.message}", LogLevel.ERROR)
        }
        return ""
    }

    fun decrypt(encryptedBase64: String): String {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
                ?: throw IllegalStateException("Private key not found")
                
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to decrypt IPC payload: ${e.message}", LogLevel.ERROR)
            return ""
        }
    }
    
    // Fallback for encrypting locally if needed (e.g., testing)
    fun encrypt(plainText: String): String {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey
                ?: throw IllegalStateException("Public key not found")
                
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to encrypt payload: ${e.message}", LogLevel.ERROR)
            return ""
        }
    }
}
