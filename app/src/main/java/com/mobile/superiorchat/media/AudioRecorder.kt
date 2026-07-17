package com.mobile.superiorchat.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.mobile.superiorchat.media.LocalDirs
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(): File? {
        val audioDir = LocalDirs.getVoiceNoteDir(context, isSent = true)
        val file = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
        currentFile = file

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            AppLog.log(LogCategory.SYSTEM, "Started recording: ${file.name}")
            return file
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to start recording: ${e.message}", LogLevel.ERROR)
            release()
            return null
        }
    }

    fun stopRecording(cancel: Boolean = false): File? {
        val file = currentFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to stop mediaRecorder: ${e.message}", LogLevel.WARN)
        } finally {
            release()
        }

        if (cancel) {
            file?.delete()
            return null
        }
        return file
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
        currentFile = null
    }
}
