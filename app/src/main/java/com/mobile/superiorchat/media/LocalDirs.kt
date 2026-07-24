package com.mobile.superiorchat.media

import android.content.Context
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import java.io.File
import java.io.IOException

object LocalDirs {

    enum class MediaType(val folderName: String) {
        IMAGE("Images"),
        VIDEO("Video"),
        AUDIO("Audio"),
        DOCUMENT("Documents"),
        VOICE_NOTE("Voice Notes")
    }

    private fun getAppName(context: Context): String {
        return context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    fun getBaseDir(context: Context): File {
        val appName = getAppName(context)
        // Use externalMediaDirs to put it in Android/media/<packagename>
        val mediaDirs = context.externalMediaDirs
        val base = if (mediaDirs.isNotEmpty() && mediaDirs[0] != null) {
            File(mediaDirs[0], "$appName/Media")
        } else {
            // Fallback
            File(context.getExternalFilesDir(null), "$appName/Media")
        }
        if (!base.exists()) {
            base.mkdirs()
        }
        return base
    }

    private fun getDir(context: Context, type: MediaType, isSent: Boolean): File {
        val base = getBaseDir(context)
        val typeDir = File(base, type.folderName)
        
        val finalDir = if (isSent) {
            File(typeDir, "Sent")
        } else {
            typeDir
        }

        if (!finalDir.exists()) {
            finalDir.mkdirs()
        }

        // Add .nomedia to Sent folders and Voice Notes so they don't pollute the gallery
        if (isSent || type == MediaType.VOICE_NOTE || type == MediaType.DOCUMENT) {
            val nomedia = File(finalDir, ".nomedia")
            if (!nomedia.exists()) {
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    // Ignore
                }
            }
        }

        return finalDir
    }

    fun getImageDir(context: Context, isSent: Boolean = false): File = getDir(context, MediaType.IMAGE, isSent)
    fun getVideoDir(context: Context, isSent: Boolean = false): File = getDir(context, MediaType.VIDEO, isSent)
    fun getAudioDir(context: Context, isSent: Boolean = false): File = getDir(context, MediaType.AUDIO, isSent)
    fun getDocumentDir(context: Context, isSent: Boolean = false): File = getDir(context, MediaType.DOCUMENT, isSent)
    fun getVoiceNoteDir(context: Context, isSent: Boolean = false): File = getDir(context, MediaType.VOICE_NOTE, isSent)

    /**
     * Efficiently searches for an existing media file in local storage.
     * 1. Unique ID match: Guaranteed matching for Telegram media across sizes.
     * 2. Message ID match: Fallback for partially downloaded/older files.
     * 3. Name + Size match: Fallback for documents without unique IDs.
     */
    fun findExistingMedia(
        context: Context,
        mediaType: String,
        fileUniqueId: String?,
        messageId: Long,
        fileName: String?,
        fileSize: Long?
    ): File? {
        val type = when (mediaType) {
            "photo" -> MediaType.IMAGE
            "video" -> MediaType.VIDEO
            "voice" -> MediaType.VOICE_NOTE
            "audio" -> MediaType.AUDIO
            else -> MediaType.DOCUMENT
        }

        val receivedDir = getDir(context, type, isSent = false)
        val sentDir = getDir(context, type, isSent = true)
        val dirsToCheck = listOf(receivedDir, sentDir)

        // 1. If we have fileUniqueId, this is the most accurate way to deduplicate Telegram media.
        if (!fileUniqueId.isNullOrBlank()) {
            val prefix = "${fileUniqueId}_"
            for (dir in dirsToCheck) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && file.name.startsWith(prefix)) {
                        return file
                    }
                }
            }
        }

        val cleanName = if (fileName != null && fileName.matches(Regex("^-?\\d+_.+"))) {
            fileName.substringAfter("_")
        } else {
            fileName
        }

        // 2. Check direct messageId match in receivedDir and sentDir
        if (!cleanName.isNullOrBlank()) {
            val candidateReceived = File(receivedDir, "${messageId}_$cleanName")
            if (candidateReceived.exists()) return candidateReceived

            val candidateSent = File(sentDir, "${messageId}_$cleanName")
            if (candidateSent.exists()) return candidateSent
        }

        // 3. Name + Size match check (Only if no fileUniqueId, or for documents)
        if (fileUniqueId.isNullOrBlank() && !cleanName.isNullOrBlank() && fileSize != null && fileSize > 0L) {
            for (dir in dirsToCheck) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && file.name != ".nomedia" && !file.name.endsWith(".tmp")) {
                        val fileDisplayName = if (file.name.matches(Regex("^-?\\d+_.+"))) file.name.substringAfter("_") else file.name
                        if (fileDisplayName.equals(cleanName, ignoreCase = true) && file.length() == fileSize) {
                            return file
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Checks if a local media file (from gallery/outbound) already exists in our app's internal folders.
     * This prevents duplicate copies when a user cancels an upload and tries sending the same file again.
     */
    fun findLocalSourceMedia(
        context: Context,
        mediaType: String,
        fileName: String?,
        fileSize: Long?
    ): File? {
        if (fileName.isNullOrBlank() || fileSize == null || fileSize <= 0L) return null

        val type = when (mediaType) {
            "photo" -> MediaType.IMAGE
            "video" -> MediaType.VIDEO
            "voice" -> MediaType.VOICE_NOTE
            "audio" -> MediaType.AUDIO
            else -> MediaType.DOCUMENT
        }

        val receivedDir = getDir(context, type, isSent = false)
        val sentDir = getDir(context, type, isSent = true)
        val dirsToCheck = listOf(sentDir, receivedDir)
        
        for (dir in dirsToCheck) {
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (file.isFile && file.name != ".nomedia" && !file.name.endsWith(".tmp")) {
                    var fileDisplayName = file.name
                    // Handle fileUniqueId_ or messageId_ prefixes
                    if (fileDisplayName.contains("_")) {
                        fileDisplayName = fileDisplayName.substringAfter("_")
                    }
                    if (fileDisplayName.equals(fileName, ignoreCase = true) && file.length() == fileSize) {
                        return file
                    }
                }
            }
        }
        return null
    }

    /**
     * Converts a File inside the app's media directory to a flavor-independent relative path for Room DB storage.
     * E.g., "/storage/.../Android/media/.../Images/Sent/123.jpg" -> "Images/Sent/123.jpg"
     */
    fun toRelativePath(context: Context, file: File): String {
        val base = getBaseDir(context).canonicalPath
        val fileCanonical = file.canonicalPath
        return if (fileCanonical.startsWith(base)) {
            fileCanonical.substring(base.length).trimStart(File.separatorChar, '/')
        } else {
            file.name
        }
    }

    /**
     * Dynamically resolves a stored path (relative or absolute) to a valid local File.
     * Guarantees backwards compatibility for absolute paths while handling flavor switches cleanly.
     */
    fun resolveFile(context: Context, path: String?): File? {
        if (path.isNullOrBlank()) return null
        
        // 1. Direct absolute file check (backwards compatibility)
        val directFile = File(path)
        if (directFile.isAbsolute && directFile.exists()) {
            return directFile
        }

        // 2. Relative path resolution from current flavor's base media dir
        val base = getBaseDir(context)
        val resolved = File(base, path)
        if (resolved.exists()) {
            return resolved
        }

        // 3. Fallback: check if path contains folder relative suffix (e.g. "Images/Sent/...")
        val cleanPath = path.trimStart(File.separatorChar, '/')
        val fallbackResolved = File(base, cleanPath)
        if (fallbackResolved.exists()) {
            return fallbackResolved
        }

        // Return candidate file even if not yet created (e.g. for pending downloads)
        return resolved
    }

    /**
     * Cleans up temporary scratch files in cacheDir (camera captures, image crops, temp voice notes).
     */
    fun clearTransientCache(context: Context) {
        try {
            val cacheDir = context.cacheDir ?: return
            val files = cacheDir.listFiles() ?: return
            var count = 0
            for (file in files) {
                if (file.name.startsWith("camera_") || 
                    file.name.startsWith("crop_") || 
                    file.name.startsWith("voice_temp_")) {
                    if (file.delete()) {
                        count++
                    }
                }
            }
            if (count > 0) {
                AppLog.log(LogCategory.SYSTEM, "LocalDirs: Cleared $count transient cache files.")
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "LocalDirs: Error clearing transient cache: ${e.message}")
        }
    }
}
