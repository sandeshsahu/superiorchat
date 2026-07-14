package com.mobile.superiorutils.utils

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
import com.mobile.superiorutils.theme.PrimaryLight
object FileUtils {

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
     * Copies a Content URI to a local file in the app's media directories.
     */
    fun copyUriToLocalFile(context: Context, uri: Uri, mediaType: String, tempMessageId: Long): File? {
        try {
            val mediaDir = when (mediaType) {
                "photo" -> com.mobile.superiorutils.media.LocalDirs.getImageDir(context, isSent = true)
                "video" -> com.mobile.superiorutils.media.LocalDirs.getVideoDir(context, isSent = true)
                "document" -> com.mobile.superiorutils.media.LocalDirs.getDocumentDir(context, isSent = true)
                "voice" -> com.mobile.superiorutils.media.LocalDirs.getVoiceNoteDir(context, isSent = true)
                "audio" -> com.mobile.superiorutils.media.LocalDirs.getAudioDir(context, isSent = true)
                else -> com.mobile.superiorutils.media.LocalDirs.getDocumentDir(context, isSent = true)
            }
            val originalName = getFileName(context, uri)
            val safeFileName = "${-tempMessageId}_$originalName"
            val localFile = File(mediaDir, safeFileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (localFile.exists()) {
                return localFile
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "FileUtils: Failed to copy URI to local file: ${e.message}")
        }
        return null
    }
}
