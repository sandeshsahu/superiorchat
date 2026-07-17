package com.mobile.superiorchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import com.mobile.superiorchat.utils.AppLog
import java.io.File
import java.io.FileOutputStream
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.ImageLoader

class SuperiorChatApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        
        setupCrashHandler()
        AppGraph.init(this)
        com.mobile.superiorchat.core.NetState.register(this)
        createNotificationChannels()
        AppLog.log(LogCategory.SYSTEM, "SuperiorChatApp initialized")
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashDir = File(getExternalFilesDir(null), "error_alrt/offline")
                if (!crashDir.exists()) crashDir.mkdirs()
                val crashFile = File(crashDir, "offline_crash.txt")
                
                val crashLog = java.lang.StringBuilder().apply {
                    appendLine("#Error")
                    appendLine("===================")
                    appendLine("⚠️ *Uncaught Exception*")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Message: ${throwable.message}")
                    appendLine("Stacktrace:")
                    appendLine(android.util.Log.getStackTraceString(throwable))
                    appendLine()
                }
                
                FileOutputStream(crashFile, true).use { fos ->
                    fos.write(crashLog.toString().toByteArray())
                }
            } catch (e: Exception) {
                android.util.Log.e("SystemCoreCrash", "Failed to write crash log", e)
            }
            
            AppLog.log(LogCategory.SYSTEM, "Uncaught exception: ${throwable.message}", LogLevel.ERROR)
            android.util.Log.e("SystemCoreCrash", "Uncaught exception", throwable)
            
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                kotlin.system.exitProcess(2)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val botServiceChannel = NotificationChannel(
                "SuperiorBotServiceChannel",
                "Background Sync",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Maintains connection for real-time messaging."
            }
            
            val incomingMessageChannel = NotificationChannel(
                "IncomingMessageChannel", 
                "Incoming Messages", 
                NotificationManager.IMPORTANCE_HIGH
            )
            
            notificationManager.createNotificationChannel(botServiceChannel)
            notificationManager.createNotificationChannel(incomingMessageChannel)
        }
    }
}
