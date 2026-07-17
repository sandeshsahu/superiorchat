package com.mobile.superiorchat.bot

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.mobile.superiorchat.camouflage.engine.Notifier as EngineNotifier
import com.mobile.superiorchat.camouflage.models.Profile

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    init {
        // Clean up any leaked chat channels that break the disguise
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("SuperiorBotServiceChannel")
            manager.deleteNotificationChannel("IncomingMessageChannel")
        }
    }

    fun getForegroundNotification(): Notification {
        // Return the silent "Idle" carrier services notification
        return EngineNotifier.buildCamouflageNotification(
            context,
            Profile.Aosp.CarrierServices(isActive = false),
            isOngoing = true
        )
    }

    fun routeUpdate(update: Update): String? {
        // Ignore the message payload. Transition the foreground notification to the "Active" state.
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = EngineNotifier.buildCamouflageNotification(
            context,
            Profile.Aosp.CarrierServices(isActive = true),
            isOngoing = false // Allow them to dismiss it, or we could keep it ongoing
        )
        manager.notify(9131, notification)
        return null
    }
}
