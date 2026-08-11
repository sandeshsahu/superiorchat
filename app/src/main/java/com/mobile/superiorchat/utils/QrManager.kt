package com.mobile.superiorchat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageProxy
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
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

data class QrConfigData(
    val token: String,
    val chatId: String,
    val autoDownloadMedia: Boolean? = null,
    val screenSecurity: Boolean? = null,
    val newMessageNotification: Boolean? = null,
    val callServer: String? = null
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
        onError: (String) -> Unit = {}
    ) {
        // Drop frame immediately if we already have a result in flight
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        try {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            // PlanarYUVLuminanceSource does not support rotateCounterClockwise() —
            // CameraX handles frame rotation via ImageAnalysis.setTargetRotation() on the use-case.
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

            // QRCodeReader is not thread-safe — synchronize across live + gallery paths
            try {
                val result = synchronized(qrReader) { qrReader.decode(binaryBitmap, decodeHints) }
                // Mark as processing immediately to block further frames
                if (isProcessing.compareAndSet(false, true)) {
                    val parsed = parseQrPayload(result.text)
                    parsed.onSuccess { data ->
                        Handler(Looper.getMainLooper()).post { onSuccess(data) }
                    }.onFailure { err ->
                        // Reset so user can try a different QR
                        isProcessing.set(false)
                        Handler(Looper.getMainLooper()).post { onError(err.message ?: "Invalid QR code.") }
                    }
                }
            } catch (e: NotFoundException) {
                // No QR in this frame — normal, keep scanning
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    // ─── Gallery / URI Scan ────────────────────────────────────────────────

    /**
     * Decodes a QR code from a URI on an IO coroutine.
     * Handles large images safely via inSampleSize subsampling.
     * Callbacks are always delivered on the Main thread.
     */
    fun processUri(
        uri: Uri,
        context: Context,
        onSuccess: (QrConfigData) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = decodeSampledBitmapFromUri(uri, context, 1024)
                    ?: run {
                        withContext(Dispatchers.Main) { onError("Could not read the selected image.") }
                        return@launch
                    }

                val bitmapWidth = bitmap.width
                val bitmapHeight = bitmap.height
                val intArray = IntArray(bitmapWidth * bitmapHeight)
                bitmap.getPixels(intArray, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
                bitmap.recycle() // safe to recycle now — pixels already copied

                val source = RGBLuminanceSource(bitmapWidth, bitmapHeight, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    // QRCodeReader is not thread-safe — synchronize
                    val result = synchronized(qrReader) { qrReader.decode(binaryBitmap, decodeHints) }
                    val parsed = parseQrPayload(result.text)
                    parsed.onSuccess { data ->
                        withContext(Dispatchers.Main) { onSuccess(data) }
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) { onError(err.message ?: "Invalid QR code.") }
                    }
                } catch (e: NotFoundException) {
                    withContext(Dispatchers.Main) { onError("No QR code found in the image.") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError("Failed to process image: ${e.localizedMessage}") }
            }
        }
    }

    // ─── Shared Payload Parser ─────────────────────────────────────────────

    /**
     * Synchronous parser — safe to call from any thread.
     * Returns Result.success(QrConfigData) or Result.failure(Exception with user-readable message).
     */
    private fun parseQrPayload(raw: String): Result<QrConfigData> {
        return try {
            val decrypted = Security.decryptAES(raw)
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

            val configData = QrConfigData(
                token = token,
                chatId = chat,
                autoDownloadMedia = json.optBoolean("autoDownloadMedia").takeIf { json.has("autoDownloadMedia") },
                screenSecurity = json.optBoolean("screenSecurity").takeIf { json.has("screenSecurity") },
                newMessageNotification = json.optBoolean("newMessageNotification").takeIf { json.has("newMessageNotification") },
                callServer = json.optString("callServer").takeIf { json.has("callServer") }
            )

            Result.success(configData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to parse QR code content."))
        }
    }

    // ─── Bitmap Sampling ───────────────────────────────────────────────────

    private fun decodeSampledBitmapFromUri(uri: Uri, context: Context, maxSize: Int): Bitmap? {
        return try {
            // First pass: read dimensions only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
            options.inJustDecodeBounds = false

            // Second pass: decode at reduced size
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
