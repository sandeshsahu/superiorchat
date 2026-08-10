package com.mobile.superiorchat.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mobile.superiorchat.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the Hidden Media Vault.
 *
 * Vault location: /sdcard/Pictures/.vault/
 * A .nomedia file inside prevents MediaStore from indexing the folder,
 * making all contents invisible to every gallery app.
 *
 * Files survive app uninstall because they live in public external storage.
 */
object VaultManager {

    private const val VAULT_FOLDER = ".vault"
    private const val NOMEDIA_FILE = ".nomedia"

    // ── Directory helpers ─────────────────────────────────────────────────────

    fun getVaultDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), VAULT_FOLDER)

    /**
     * Creates the vault folder and .nomedia sentinel on first call.
     * Safe to call repeatedly — no-op if already exists.
     */
    fun ensureVaultReady() {
        val dir = getVaultDir()
        if (!dir.exists()) dir.mkdirs()
        val nomedia = File(dir, NOMEDIA_FILE)
        if (!nomedia.exists()) nomedia.createNewFile()
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns all vault items by scanning the vault directory directly.
     * This means items persist even after app data is cleared — the folder is source of truth.
     * Prefs is kept in sync as a convenience index but is NOT required to list items.
     */
    fun getVaultItems(prefs: Prefs): List<File> {
        val dir = getVaultDir()
        val filesOnDisk = if (dir.exists()) {
            dir.listFiles { f -> f.isFile && f.name != NOMEDIA_FILE }
                ?.toList() ?: emptyList()
        } else emptyList()

        // Rebuild prefs to match what's actually on disk (heals data-clear state)
        val diskPaths = filesOnDisk.map { it.absolutePath }.toSet()
        if (diskPaths != prefs.vaultPaths) {
            prefs.vaultPaths = diskPaths
        }

        return filesOnDisk.sortedByDescending { it.lastModified() }
    }

    // ── Hide ──────────────────────────────────────────────────────────────────

    /**
     * Copies a media URI into the vault, deletes the original from MediaStore,
     * and records the vault path in [prefs].
     *
     * @return true on success, false on any failure (original left untouched on failure).
     */
    suspend fun hideMedia(context: Context, uri: Uri, prefs: Prefs): Boolean =
        withContext(Dispatchers.IO) {
            try {
                ensureVaultReady()
                val fileName = resolveDisplayName(context, uri)
                    ?: "vault_${System.currentTimeMillis()}"
                val destFile = uniqueFile(getVaultDir(), fileName)

                // 1. Copy bytes
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                    true
                } ?: false

                if (!copied) return@withContext false

                // 2. Delete original
                var fileDeleted = false
                try {
                    if (uri.scheme == "content") {
                        context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DATA),
                            null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val path = cursor.getString(0)
                                if (path != null) {
                                    val f = File(path)
                                    if (f.exists()) {
                                        fileDeleted = f.delete()
                                    }
                                }
                            }
                        }
                    } else if (uri.scheme == "file") {
                        val path = uri.path
                        if (path != null) {
                            val f = File(path)
                            if (f.exists()) {
                                fileDeleted = f.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignored
                }

                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: SecurityException) {
                    // On Android 11, MediaStore delete can throw if we don't own the file,
                    // but if we deleted it via File API (thanks to MANAGE_EXTERNAL_STORAGE), we're good.
                    if (!fileDeleted) throw e
                } catch (e: Exception) {
                    if (!fileDeleted) throw e
                }

                // 3. Track in prefs
                prefs.vaultPaths = prefs.vaultPaths + destFile.absolutePath

                true
            } catch (e: Exception) {
                false
            }
        }

    // ── Unhide ────────────────────────────────────────────────────────────────

    /**
     * Moves a vault file back into the public Pictures folder and removes it
     * from [prefs].
     *
     * @return true on success.
     */
    suspend fun unhideMedia(context: Context, filePath: String, prefs: Prefs): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val srcFile = File(filePath)
                if (!srcFile.exists()) {
                    prefs.vaultPaths = prefs.vaultPaths - filePath
                    return@withContext false
                }

                val fileName = srcFile.name
                val mimeType = getMimeType(fileName)
                val isVideo = mimeType?.startsWith("video") == true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: write via MediaStore (no extra permissions needed)
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType ?: "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val collection = if (isVideo)
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                    val destUri = context.contentResolver.insert(collection, values)
                        ?: return@withContext false

                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        srcFile.inputStream().use { it.copyTo(output) }
                    }
                } else {
                    // Android 9 and below: direct file copy
                    val picturesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    )
                    srcFile.copyTo(uniqueFile(picturesDir, fileName), overwrite = false)
                }

                srcFile.delete()
                prefs.vaultPaths = prefs.vaultPaths - filePath
                true
            } catch (e: Exception) {
                false
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true if the file extension is a known video format. */
    fun isVideoFile(file: File): Boolean {
        return getMimeType(file.name)?.startsWith("video") == true
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? =
        try {
            if (uri.scheme == "file") {
                return uri.lastPathSegment
            }
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }

    /** Ensures no filename collision in the destination directory. */
    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        var counter = 1
        val ext = name.substringAfterLast('.', "")
        val base = if (ext.isNotEmpty()) name.removeSuffix(".$ext") else name
        while (f.exists()) {
            f = File(dir, if (ext.isNotEmpty()) "${base}_${counter}.${ext}" else "${base}_${counter}")
            counter++
        }
        return f
    }

    private fun getMimeType(fileName: String): String? =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png"         -> "image/png"
            "gif"         -> "image/gif"
            "webp"        -> "image/webp"
            "heic"        -> "image/heic"
            "mp4"         -> "video/mp4"
            "mkv"         -> "video/x-matroska"
            "avi"         -> "video/avi"
            "mov"         -> "video/quicktime"
            "3gp"         -> "video/3gpp"
            else          -> null
        }
}
