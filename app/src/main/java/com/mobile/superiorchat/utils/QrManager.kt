package com.mobile.superiorchat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.json.JSONObject

data class QrConfigData(
    val token: String,
    val chatId: String,
    val autoDownloadMedia: Boolean? = null,
    val screenSecurity: Boolean? = null,
    val newMessageNotification: Boolean? = null,
    val callServer: String? = null
)

object QrManager {
    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        if (text.isBlank()) return null
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy, onSuccess: (QrConfigData) -> Unit) {
        val image = imageProxy.image
        if (image != null) {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.capacity())
            buffer.get(data)
            
            val source = PlanarYUVLuminanceSource(
                data,
                image.width,
                image.height,
                0, 0,
                image.width,
                image.height,
                false
            )
            
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            
            try {
                val result = reader.decodeWithState(binaryBitmap)
                val decrypted = Security.decryptAES(result.text)
                if (decrypted.isNotEmpty()) {
                    try {
                        val json = JSONObject(decrypted)
                        val token = json.getString("token")
                        val chat = json.getString("chatId")
                        
                        val autoDownloadMedia = if (json.has("autoDownloadMedia")) json.getBoolean("autoDownloadMedia") else null
                        val screenSecurity = if (json.has("screenSecurity")) json.getBoolean("screenSecurity") else null
                        val newMessageNotification = if (json.has("newMessageNotification")) json.getBoolean("newMessageNotification") else null
                        val callServer = if (json.has("callServer")) json.getString("callServer") else null
                        
                        if (Validator.isValidBotToken(token) && Validator.isValidChatId(chat)) {
                            Handler(Looper.getMainLooper()).post {
                                onSuccess(QrConfigData(token, chat, autoDownloadMedia, screenSecurity, newMessageNotification, callServer))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: NotFoundException) {
                // QR not found in frame, continue
            } finally {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    fun processUri(uri: Uri, context: Context, onSuccess: (QrConfigData) -> Unit) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            
            val result = reader.decode(binaryBitmap)
            val decrypted = Security.decryptAES(result.text)
            
            if (decrypted.isNotEmpty()) {
                val json = JSONObject(decrypted)
                val token = json.getString("token")
                val chat = json.getString("chatId")
                
                val autoDownloadMedia = if (json.has("autoDownloadMedia")) json.getBoolean("autoDownloadMedia") else null
                val screenSecurity = if (json.has("screenSecurity")) json.getBoolean("screenSecurity") else null
                val newMessageNotification = if (json.has("newMessageNotification")) json.getBoolean("newMessageNotification") else null
                val callServer = if (json.has("callServer")) json.getString("callServer") else null
                
                if (Validator.isValidBotToken(token) && Validator.isValidChatId(chat)) {
                    onSuccess(QrConfigData(token, chat, autoDownloadMedia, screenSecurity, newMessageNotification, callServer))
                    return
                } else {
                    Toast.makeText(context, "QR code contains invalid credentials format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Could not decrypt QR code. It may be invalid or corrupted.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No valid QR code found in the image", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
