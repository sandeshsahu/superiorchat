package com.mobile.superiorsetup.core

import android.util.Base64
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object Security {
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

    fun encryptRSA(plainText: String, publicKeyBase64: String): String {
        try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)
            
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val CONSTANT_SECRET = "SuperiorChat_QR_Secret_V1"

    private fun getAESKey(): javax.crypto.SecretKey {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(CONSTANT_SECRET.toByteArray(Charsets.UTF_8))
        return javax.crypto.spec.SecretKeySpec(hash, "AES")
    }

    fun encryptAES(plainText: String): String {
        try {
            val key = getAESKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            
            // GCM recommends 12 byte IV
            val iv = ByteArray(12)
            java.security.SecureRandom().nextBytes(iv)
            val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Prepend IV to ciphertext for decryption
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decryptAES(encryptedBase64: String): String {
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)
            
            val key = getAESKey()
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
