package com.mobile.superiorutils.data.repository

import android.content.Context
import android.net.Uri
import com.mobile.superiorutils.data.dao.ThreadDao
import com.mobile.superiorutils.data.dao.MessageDao
import com.mobile.superiorutils.data.entity.ChatNode
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.bot.TelegramApi
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogCategory
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalMediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val duration: String? = null,
    val dateAdded: Long,
    val bucketName: String = "All"
)

data class LocalFileItem(
    val name: String,
    val path: String,
    val size: String,
    val mimeType: String?,
    val isDirectory: Boolean = false,
    val dateModified: String
)

class AppRepository(
    private val conversationDao: ThreadDao,
    private val messageDao: MessageDao
) {
    fun getAllConversations(): Flow<List<ChatNode>> {
        return conversationDao.getAllConversations()
    }

    fun getMessagesForConversation(chatId: String, limit: Int): Flow<List<MessageNode>> {
        return messageDao.getMessagesForConversation(chatId, limit)
    }

    suspend fun insertMessage(message: MessageNode) {
        messageDao.insertMessage(message)
    }

    suspend fun insertOrUpdateConversation(conversation: ChatNode) {
        val id = conversationDao.insertConversation(conversation)
        if (id == -1L) {
            conversationDao.updateConversation(conversation)
        }
    }

    suspend fun ensureConversationExists(chatId: String) {
        val chatNode = ChatNode(
            chatId = chatId,
            title = "Chat",
            lastMessageText = null,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        conversationDao.insertConversation(chatNode)
    }

    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status)
     }

    suspend fun getQueuedMessages(): List<MessageNode> {
        return messageDao.getMessagesByStatus(MessageStatus.QUEUED)
    }

    suspend fun sendTextMessage(token: String, chatId: String, text: String, tempMessageId: Long): Boolean {
        val sentId = TelegramApi.sendMessage(token, chatId, text)
        return if (sentId != null) {
            updateMessageStatus(tempMessageId, MessageStatus.SENT)
            true
        } else {
            updateMessageStatus(tempMessageId, MessageStatus.FAILED)
            false
        }
    }

    private fun isUselessFile(name: String, path: String): Boolean {
        if (name.startsWith(".")) return true
        val ext = name.substringAfterLast(".").lowercase()
        val excludedExtensions = setOf("db", "db-shm", "db-wal", "nomedia", "tmp", "temp", "log", "json", "ini", "properties")
        if (ext in excludedExtensions) return true
        val lowercasePath = path.lowercase()
        if (lowercasePath.contains("com.mobile.superiorutils") || 
            lowercasePath.contains("/android/") || 
            lowercasePath.contains("/android") || 
            lowercasePath.contains("/.thumbnails/") ||
            lowercasePath.contains("/cache/")
        ) return true
        return false
    }

    fun getRecentImages(context: Context): List<Uri> {
        val uriList = mutableListOf<Uri>()
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATE_ADDED
            )
            val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
            
            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && uriList.size < 20) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    uriList.add(contentUri)
                }
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "Failed to load recent images: ${e.message}")
        }
        return uriList
    }

    fun getAllLocalMedia(context: Context): List<LocalMediaItem> {
        val mediaList = mutableListOf<LocalMediaItem>()
        val contentResolver = context.contentResolver

        val sortOrder = "date_added DESC"
        // 1. Query Images
        val imageProjection = arrayOf(
            android.provider.MediaStore.Images.Media._ID,
            android.provider.MediaStore.Images.Media.DATE_ADDED,
            android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val imageUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        try {
            contentResolver.query(imageUri, imageProjection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext() && mediaList.size < 150) {
                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "All" else "All"
                    val uri = android.content.ContentUris.withAppendedId(imageUri, id)
                    mediaList.add(LocalMediaItem(id, uri, false, null, date, bucket))
                }
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "Error querying local images: ${e.message}")
        }

        // 2. Query Videos
        val videoProjection = arrayOf(
            android.provider.MediaStore.Video.Media._ID,
            android.provider.MediaStore.Video.Media.DURATION,
            android.provider.MediaStore.Video.Media.DATE_ADDED,
            android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val videoUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        try {
            contentResolver.query(videoUri, videoProjection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                var videoCount = 0
                while (cursor.moveToNext() && videoCount < 150) {
                    val id = cursor.getLong(idCol)
                    val durationMs = cursor.getLong(durCol)
                    val date = cursor.getLong(dateCol)
                    val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "All" else "All"
                    val uri = android.content.ContentUris.withAppendedId(videoUri, id)
                    val sec = (durationMs / 1000) % 60
                    val min = (durationMs / 1000) / 60
                    val durationStr = String.format(Locale.getDefault(), "%d:%02d", min, sec)
                    mediaList.add(LocalMediaItem(id, uri, true, durationStr, date, bucket))
                    videoCount++
                }
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "Error querying local videos: ${e.message}")
        }

        mediaList.sortByDescending { it.dateAdded }
        return mediaList
    }

    fun getRecentFiles(context: Context): List<LocalFileItem> {
        val fileList = mutableListOf<LocalFileItem>()
        val projection = arrayOf(
            android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME,
            android.provider.MediaStore.Files.FileColumns.DATA,
            android.provider.MediaStore.Files.FileColumns.SIZE,
            android.provider.MediaStore.Files.FileColumns.MIME_TYPE,
            android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val selection = "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = ${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"
        val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        
        try {
            context.contentResolver.query(
                android.provider.MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED)
                
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                while (cursor.moveToNext() && fileList.size < 30) {
                    val name = cursor.getString(nameCol) ?: "Unknown"
                    val path = cursor.getString(dataCol) ?: ""
                    if (isUselessFile(name, path)) continue
                    
                    val sizeBytes = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol)
                    val dateSec = cursor.getLong(dateCol)
                    
                    val sizeStr = when {
                        sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                        sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                        else -> "$sizeBytes B"
                    }
                    val dateStr = sdf.format(Date(dateSec * 1000))
                    
                    fileList.add(LocalFileItem(name, path, sizeStr, mime, false, dateStr))
                }
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.ERROR, "Error querying files: ${e.message}")
        }
        
        if (fileList.isEmpty()) {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val files = downloadDir.listFiles()
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                
                val validFiles = files?.filter { it.isFile && !isUselessFile(it.name, it.absolutePath) }
                    ?.sortedByDescending { it.lastModified() }
                    ?.take(10)
                    
                validFiles?.forEach { file ->
                    val sizeBytes = file.length()
                    val sizeStr = when {
                        sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                        sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                        else -> "$sizeBytes B"
                    }
                    val dateStr = sdf.format(Date(file.lastModified()))
                    val mime = context.contentResolver.getType(Uri.fromFile(file))
                    fileList.add(LocalFileItem(file.name, file.absolutePath, sizeStr, mime, false, dateStr))
                }
            }
        }
        return fileList.take(5)
    }

    fun getFilesInDirectory(context: Context, directory: File): List<LocalFileItem> {
        val list = mutableListOf<LocalFileItem>()
        var files = directory.listFiles()
        
        if (files == null && directory.absolutePath == android.os.Environment.getExternalStorageDirectory().absolutePath) {
            val publicDirs = arrayOf(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                android.os.Environment.DIRECTORY_DOCUMENTS,
                android.os.Environment.DIRECTORY_DCIM,
                android.os.Environment.DIRECTORY_PICTURES,
                android.os.Environment.DIRECTORY_MUSIC,
                android.os.Environment.DIRECTORY_MOVIES
            )
            val fallbackList = mutableListOf<File>()
            publicDirs.forEach { dirName ->
                val dir = android.os.Environment.getExternalStoragePublicDirectory(dirName)
                if (dir.exists()) {
                    fallbackList.add(dir)
                }
            }
            files = fallbackList.toTypedArray()
        }
        
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        files?.forEach { file ->
            if (isUselessFile(file.name, file.absolutePath)) return@forEach
            val sizeBytes = file.length()
            val sizeStr = when {
                file.isDirectory -> ""
                sizeBytes > 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
                sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                else -> "$sizeBytes B"
            }
            val dateStr = sdf.format(Date(file.lastModified()))
            val mime = if (file.isFile) context.contentResolver.getType(Uri.fromFile(file)) else null
            list.add(LocalFileItem(file.name, file.absolutePath, sizeStr, mime, file.isDirectory, dateStr))
        }
        list.sortWith(compareBy<LocalFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        return list
    }
}
