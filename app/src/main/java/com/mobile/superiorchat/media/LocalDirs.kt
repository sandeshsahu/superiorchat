package com.mobile.superiorchat.media

import android.content.Context
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
}
