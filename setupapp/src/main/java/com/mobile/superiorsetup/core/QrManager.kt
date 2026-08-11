package com.mobile.superiorsetup.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.camera.core.ImageProxy
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

data class QrConfigData(
    val token: String,
    val chatId: String,
    val autoDownloadMedia: Boolean,
    val screenSecurity: Boolean,
    val newMessageNotification: Boolean,
    val callServer: String
)

object QrManager {

    // Reused reader with QR_CODE-only hints for performance
    private val qrReader = QRCodeReader()
    private val decodeHints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    // Debounce flag — prevents onSuccess firing multiple times per scan session
    private val isProcessing = AtomicBoolean(false)

    /** Call this when the scanner is opened to reset debounce state. */
    fun resetState() {
        isProcessing.set(false)
    }

    // ─── QR Code Generation ────────────────────────────────────────────────

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

    // ─── Live Camera Scan ──────────────────────────────────────────────────

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processImageProxy(
        imageProxy: ImageProxy,
        onSuccess: (QrConfigData) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            if (isProcessing.get()) {
                imageProxy.close()
                return
            }

            val image = imageProxy.image
            if (image == null) {
                imageProxy.close()
                return
            }

            val buffer = imageProxy.planes[0].buffer
            val remaining = buffer.remaining()
            val data = ByteArray(remaining)
            buffer.get(data)
            
            val source = PlanarYUVLuminanceSource(
                data,
                imageProxy.width,
                imageProxy.height,
                0, 0,
                imageProxy.width,
                imageProxy.height,
                false
            )
            
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            try {
                val result = synchronized(qrReader) { qrReader.decode(binaryBitmap, decodeHints) }
                
                if (isProcessing.compareAndSet(false, true)) {
                    val parsed = parseQrPayload(result.text)
                    parsed.onSuccess { data ->
                        Handler(Looper.getMainLooper()).post { onSuccess(data) }
                    }.onFailure { err ->
                        isProcessing.set(false)
                        Handler(Looper.getMainLooper()).post { onError(err) }
                    }
                }
            } catch (e: NotFoundException) {
                // Keep scanning
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    // ─── Gallery / URI Scan ────────────────────────────────────────────────

    fun processUri(
        uri: Uri,
        context: Context,
        onSuccess: (QrConfigData) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = decodeSampledBitmapFromUri(uri, context, 1024)
                    ?: run {
                        withContext(Dispatchers.Main) { onError(Exception("Could not read the selected image.")) }
                        return@launch
                    }

                val bitmapWidth = bitmap.width
                val bitmapHeight = bitmap.height
                val intArray = IntArray(bitmapWidth * bitmapHeight)
                bitmap.getPixels(intArray, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
                bitmap.recycle()

                val source = RGBLuminanceSource(bitmapWidth, bitmapHeight, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val result = synchronized(qrReader) { qrReader.decode(binaryBitmap, decodeHints) }
                    val parsed = parseQrPayload(result.text)
                    parsed.onSuccess { data ->
                        withContext(Dispatchers.Main) { onSuccess(data) }
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) { onError(err) }
                    }
                } catch (e: NotFoundException) {
                    withContext(Dispatchers.Main) { onError(Exception("No QR code found in the image.")) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError(Exception("Failed to process image: ${e.localizedMessage}")) }
            }
        }
    }

    // ─── Shared Payload Parser ─────────────────────────────────────────────

    class PinRequiredException(val rawPayload: String) : Exception("PIN required")

    fun parseDecryptedJson(decrypted: String): Result<QrConfigData> {
        return try {
            if (decrypted.isEmpty()) {
                return Result.failure(Exception("QR code is not a valid Superior Chat config."))
            }

            val json = JSONObject(decrypted)

            if (!json.has("token") || !json.has("chatId")) {
                return Result.failure(Exception("QR code is missing required fields."))
            }

            val token = json.getString("token")
            val chat = json.getString("chatId")

            if (!Validator.isValidBotToken(token)) {
                return Result.failure(Exception("Invalid bot token in QR code."))
            }
            if (!Validator.isValidChatId(chat)) {
                return Result.failure(Exception("Invalid chat ID in QR code."))
            }

            Result.success(
                QrConfigData(
                    token = token,
                    chatId = chat,
                    autoDownloadMedia = json.optBoolean("autoDownloadMedia", false),
                    screenSecurity = json.optBoolean("screenSecurity", true),
                    newMessageNotification = json.optBoolean("newMessageNotification", true),
                    callServer = json.optString("callServer", "")
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to parse QR code content."))
        }
    }

    fun parseQrPayload(raw: String): Result<QrConfigData> {
        return try {
            if (raw.startsWith("SEC_QR:")) {
                return Result.failure(PinRequiredException(raw))
            } else if (raw.startsWith("DIR_QR:")) {
                val decrypted = Security.decryptAES(raw)
                if (decrypted.isEmpty()) return Result.failure(Exception("Failed to decrypt setup configuration."))
                return parseDecryptedJson(decrypted)
            } else {
                return Result.failure(Exception("Invalid or unsupported QR Code"))
            }
        } catch (e: PinRequiredException) {
            Result.failure(e)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to parse QR code content."))
        }
    }

    // ─── Bitmap Sampling ───────────────────────────────────────────────────

    private fun decodeSampledBitmapFromUri(uri: Uri, context: Context, maxSize: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
