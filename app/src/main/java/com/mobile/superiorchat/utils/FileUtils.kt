package com.mobile.superiorchat.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mobile.superiorchat.theme.PrimaryLight

object FileUtils {

    /**
     * Safely deletes a file without throwing uncaught exceptions.
     */
    fun deleteQuietly(file: File?): Boolean {
        if (file == null || !file.exists()) return false
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Accurately gets the file size in bytes from either a content:// URI or a file:// URI.
     * Returns 0L if the file cannot be accessed or measured.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) return size
                    }
                }
                
                // Fallback: try opening file descriptor if cursor fails
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val size = pfd.statSize
                    if (size > 0) return size
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "FileUtils: Failed to read content URI size: ${e.message}")
            }
        } else if (uri.scheme == "file" || uri.scheme == null) {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    return file.length()
                }
            }
        }
        return 0L
    }

    /**
     * Formats bytes into a human-readable string (e.g., 55.2 MB, 800 KB).
     */
    fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
            sizeBytes >= 1024 -> "${sizeBytes / 1024} KB"
            else -> "$sizeBytes B"
        }
    }

    /**
     * Extracts the file name from a given URI.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.log(LogCategory.ERROR, "FileUtils: Failed to get file name: ${e.message}")
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown"
    }

    /**
     * Returns an appropriate Material Icon based on the file extension.
     */
    fun resolveFileIcon(filename: String): ImageVector {
        val ext = filename.substringAfterLast(".", "").lowercase(Locale.ROOT)
        return when (ext) {
            "pdf" -> Icons.Default.PictureAsPdf
            "apk" -> Icons.Default.Android
            "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
            "doc", "docx", "txt", "rtf", "log" -> Icons.Default.Description
            "xls", "xlsx", "csv" -> Icons.Default.TableChart
            "ppt", "pptx" -> Icons.Default.Slideshow
            "mp3", "wav", "ogg", "flac" -> Icons.Default.AudioFile
            "mp4", "mkv", "avi", "mov" -> Icons.Default.VideoFile
            "jpg", "jpeg", "png", "gif", "webp" -> Icons.Default.Image
            "kt", "java", "py", "json", "xml", "html", "js", "css" -> Icons.Default.Code
            else -> Icons.Default.Description
        }
    }

    /**
     * Returns an appropriate Color based on the file extension.
     */
    fun resolveFileIconColor(filename: String): Color {
        val ext = filename.substringAfterLast(".", "").lowercase(Locale.ROOT)
        return when (ext) {
            "pdf" -> Color(0xFFFF8B8B) // Light Red
            "apk" -> Color(0xFF8BFFB5) // Android Green
            "zip", "rar", "7z", "tar", "gz" -> Color(0xFFFFC08B) // Archive Orange
            "doc", "docx", "txt", "rtf", "log" -> Color(0xFF8BBAFF) // Light Blue
            "xls", "xlsx", "csv" -> Color(0xFF8BFF9B) // Light Green
            "ppt", "pptx" -> Color(0xFFFF9B8B) // Presentation Red/Orange
            "mp3", "wav", "ogg", "flac" -> Color(0xFFD68BFF) // Audio Purple
            "mp4", "mkv", "avi", "mov" -> Color(0xFFFF8B8B) // Video Red
            "jpg", "jpeg", "png", "gif", "webp" -> Color(0xFFFFDB8B) // Image Yellow
            "kt", "java", "py", "json", "xml", "html", "js", "css" -> Color(0xFF8BFFF0) // Code Teal
            else -> PrimaryLight
        }
    }

    /**
     * Determines the internal media type string (audio, video, photo, document) from an extension.
     */
    fun getMediaType(filename: String): String {
        val ext = filename.substringAfterLast(".", "").lowercase(Locale.ROOT)
        return when (ext) {
            "mp3", "wav", "ogg", "flac" -> "audio"
            "mp4", "mkv", "avi", "mov" -> "video"
            "jpg", "jpeg", "png", "gif", "webp" -> "photo"
            else -> "document"
        }
    }

    /**
     * Copies a Content/File URI to a permanent local file in the app's media directories.
     * Prevents duplicate copying if file is already inside app storage, and immediately purges
     * temporary cache files (like camera_*.jpg or crop_*.jpg) after copying to Sent/.
     */
    fun copyUriToLocalFile(context: Context, uri: Uri, mediaType: String, tempMessageId: Long): File? {
        try {
            val mediaDir = when (mediaType) {
                "photo" -> com.mobile.superiorchat.media.LocalDirs.getImageDir(context, isSent = true)
                "video" -> com.mobile.superiorchat.media.LocalDirs.getVideoDir(context, isSent = true)
                "document" -> com.mobile.superiorchat.media.LocalDirs.getDocumentDir(context, isSent = true)
                "voice" -> com.mobile.superiorchat.media.LocalDirs.getVoiceNoteDir(context, isSent = true)
                "audio" -> com.mobile.superiorchat.media.LocalDirs.getAudioDir(context, isSent = true)
                else -> com.mobile.superiorchat.media.LocalDirs.getDocumentDir(context, isSent = true)
            }

            // Check if source file is already a File located inside app's media storage
            if (uri.scheme == "file" || uri.scheme == null) {
                val sourcePath = uri.path
                if (sourcePath != null) {
                    val sourceFile = File(sourcePath)
                    val baseMediaDir = com.mobile.superiorchat.media.LocalDirs.getBaseDir(context).canonicalPath
                    if (sourceFile.exists() && sourceFile.canonicalPath.startsWith(baseMediaDir)) {
                        // File is already in app storage; reuse directly without duplicating!
                        return sourceFile
                    }
                }
            }

            val originalName = getFileName(context, uri)
            val safeFileName = "${-tempMessageId}_$originalName"
            val localFile = File(mediaDir, safeFileName)
            val expectedSize = getFileSize(context, uri)

            // Deduplication check: if file already exists in target with matching size, reuse
            if (localFile.exists() && expectedSize > 0L && localFile.length() == expectedSize) {
                return localFile
            }
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (localFile.exists()) {
                // If source was a temporary file in cacheDir (e.g. camera capture or image crop), delete it now
                if (uri.scheme == "file" || uri.scheme == null) {
                    val sourcePath = uri.path
                    if (sourcePath != null) {
                        val sourceFile = File(sourcePath)
                        val cachePath = context.cacheDir?.canonicalPath
                        if (cachePath != null && sourceFile.canonicalPath.startsWith(cachePath)) {
                            deleteQuietly(sourceFile)
                            AppLog.log(LogCategory.SYSTEM, "FileUtils: Purged temporary cache file after copying to Sent: ${sourceFile.name}")
                        }
                    }
                }
                return localFile
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "FileUtils: Failed to copy URI to local file: ${e.message}")
        }
        return null
    }

    /**
     * Safely loads an image, applies a fractional crop box, scales it to a target size,
     * and saves it to a temporary file. Prevents OOM by downsampling before cropping.
     */
    suspend fun cropAndScaleImage(
        context: Context,
        uri: Uri,
        cropX: Float,
        cropY: Float,
        cropSize: Float,
        targetWidth: Int = 512,
        targetHeight: Int = 512,
        maxLoadSize: Int = 2048
    ): File? {
        return try {
            val loader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(uri)
                .size(maxLoadSize)
                .allowHardware(false)
                .build()
                
            val result = loader.execute(request)
            if (result is coil.request.SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val width = bitmap.width
                    val height = bitmap.height
                    val minDim = Math.min(width, height)
                    
                    val pixelCropX = (cropX * width).toInt().coerceIn(0, width - 1)
                    val pixelCropY = (cropY * height).toInt().coerceIn(0, height - 1)
                    val maxPossibleSize = Math.min(width - pixelCropX, height - pixelCropY)
                    val pixelCropSize = (cropSize * minDim).toInt().coerceAtMost(maxPossibleSize)
                    
                    val croppedBitmap = android.graphics.Bitmap.createBitmap(bitmap, pixelCropX, pixelCropY, pixelCropSize, pixelCropSize)
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
                    
                    val tempFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
                    var outputStream: java.io.FileOutputStream? = null
                    try {
                        outputStream = java.io.FileOutputStream(tempFile)
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                    } finally {
                        outputStream?.close()
                        if (croppedBitmap != bitmap) croppedBitmap.recycle()
                        if (scaledBitmap != croppedBitmap && scaledBitmap != bitmap) scaledBitmap.recycle()
                    }
                    
                    tempFile
                } else null
            } else null
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "FileUtils: Failed to crop image: ${e.message}")
            null
        }
    }

    /**
     * Exports a file directly to the system's public galleries (Pictures, Movies, etc.).
     * Prevents usage of app name to maintain privacy.
     */
    fun exportMediaToGallery(context: Context, sourceFile: File, mediaType: String, originalName: String? = null): Boolean {
        if (!sourceFile.exists()) return false
        try {
            val displayName = originalName ?: sourceFile.name
            val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    
                    val relativePath = when (mediaType) {
                        "photo" -> android.os.Environment.DIRECTORY_PICTURES
                        "video" -> android.os.Environment.DIRECTORY_MOVIES
                        "audio", "voice" -> android.os.Environment.DIRECTORY_MUSIC
                        else -> android.os.Environment.DIRECTORY_DOWNLOADS
                    }
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = when (mediaType) {
                    "photo" -> android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    "video" -> android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    "audio", "voice" -> android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else -> android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val uri = context.contentResolver.insert(collection, contentValues) ?: return false

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            } else {
                val publicDir = when (mediaType) {
                    "photo" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    "video" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                    "audio", "voice" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                    else -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                if (!publicDir.exists()) publicDir.mkdirs()
                val targetFile = File(publicDir, displayName)
                sourceFile.copyTo(targetFile, overwrite = true)
            }
            return true
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "FileUtils: Failed to export to gallery: ${e.message}")
            return false
        }
    }
}
