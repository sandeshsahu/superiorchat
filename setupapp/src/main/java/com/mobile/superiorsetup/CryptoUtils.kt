package com.mobile.superiorsetup

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private val IPC_SECRET_KEY = byteArrayOf(
        0x53, 0x75, 0x70, 0x65, 0x72, 0x69, 0x6F, 0x72, 0x43, 0x68, 0x61, 0x74, 0x53, 0x65, 0x63, 0x72,
        0x65, 0x74, 0x4B, 0x65, 0x79, 0x32, 0x30, 0x32, 0x34, 0x5F, 0x30, 0x30, 0x37, 0x21, 0x40, 0x23
    )

    fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTE)
            java.security.SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            val secretKeySpec = SecretKeySpec(IPC_SECRET_KEY, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun decrypt(encryptedText: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val cipherText = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, iv.size, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            val secretKeySpec = SecretKeySpec(IPC_SECRET_KEY, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec)
            val plainText = cipher.doFinal(cipherText)
            return String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
