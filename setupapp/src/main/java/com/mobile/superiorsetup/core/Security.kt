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
            
            return "DIR_QR:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decryptAES(encryptedPayload: String): String {
        try {
            if (!encryptedPayload.startsWith("DIR_QR:")) return ""
            val b64 = encryptedPayload.removePrefix("DIR_QR:")
            val combined = Base64.decode(b64, Base64.NO_WRAP)
            if (combined.size < 12) return ""
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

    private fun deriveKeyFromPin(pin: String, salt: ByteArray): javax.crypto.SecretKey {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val combinedSecret = pin + CONSTANT_SECRET
        val spec = javax.crypto.spec.PBEKeySpec(combinedSecret.toCharArray(), salt, 65536, 256)
        val secret = factory.generateSecret(spec)
        return javax.crypto.spec.SecretKeySpec(secret.encoded, "AES")
    }

    fun encryptAESWithPin(plainText: String, pin: String): String {
        try {
            val salt = ByteArray(16)
            java.security.SecureRandom().nextBytes(salt)
            
            val key = deriveKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            
            val iv = ByteArray(12)
            java.security.SecureRandom().nextBytes(iv)
            val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Format: Salt (16) || IV (12) || Ciphertext
            val combined = ByteArray(salt.size + iv.size + encryptedBytes.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(iv, 0, combined, salt.size, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, salt.size + iv.size, encryptedBytes.size)
            
            // Prefix to identify as a PIN secured QR
            return "SEC_QR:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decryptAESWithPin(payload: String, pin: String): String {
        try {
            if (!payload.startsWith("SEC_QR:")) return ""
            val b64 = payload.removePrefix("SEC_QR:")
            val combined = Base64.decode(b64, Base64.NO_WRAP)
            
            if (combined.size < 28) return "" // Salt(16) + IV(12)
            
            val salt = ByteArray(16)
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 28)
            
            System.arraycopy(combined, 0, salt, 0, 16)
            System.arraycopy(combined, 16, iv, 0, 12)
            System.arraycopy(combined, 28, encryptedBytes, 0, encryptedBytes.size)
            
            val key = deriveKeyFromPin(pin, salt)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "" // Decryption failed (wrong PIN or corrupt data)
        }
    }
}
