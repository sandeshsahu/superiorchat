package com.mobile.superiorchat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mobile.superiorchat.bot.BotSync
import com.mobile.superiorchat.core.ServiceCore
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogLevel

class BotService : Service() {

    private lateinit var botManager: BotSync
    private val NOTIFICATION_ID = 9131

    override fun onCreate() {
        super.onCreate()
        botManager = BotSync(this)
        AppLog.log(LogCategory.SYSTEM, "BotService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = AppGraph.prefs
        if (!prefs.isConfigured) {
            AppLog.log(LogCategory.SYSTEM, "Credentials not configured. Stopping self.")
            stopSelf()
            return START_NOT_STICKY
        }

        AppLog.setServiceRunning(true)
        startForegroundServiceNotification()
        
        botManager.startPolling()
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        botManager.stopPolling()
        AppLog.setServiceRunning(false)
        AppLog.log(LogCategory.SYSTEM, "BotService onDestroy")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Re-start the service so polling survives app swipe-away
        ServiceCore.ensureRunning(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val notification = botManager.notifier.getForegroundNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "BotService: startForeground failed: ${e.message}", LogLevel.ERROR)
            stopSelf()
        }
    }
}
