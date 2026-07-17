package com.mobile.superiorchat.camouflage.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.Profile
import com.mobile.superiorchat.camouflage.ui.DecoyActivity

import android.app.Notification

object Notifier {

    fun showTestCamouflage(context: Context, profile: Profile) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildCamouflageNotification(context, profile, false)
        manager.notify(9131, notification)
    }

    fun buildCamouflageNotification(context: Context, profile: Profile, isOngoing: Boolean = false): Notification {
        val data = Manager.resolveCamouflage(context, profile)
        
        val channelId = "camo_channel_v3_${profile.javaClass.simpleName}"
        
        // 1. Create Spoofed Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (profile is Profile.Aosp.CarrierServices) "Carrier Services" else data.appNameSpoof
            val channel = NotificationChannel(
                channelId,
                channelName, // This overrides the App Name on newer Androids in Settings
                if (data.isSilent) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System notifications"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 2. Create the Decoy Intent
        val decoyIntent = Intent(context, DecoyActivity::class.java).apply {
            putExtra(DecoyActivity.EXTRA_INTENT_ACTION, data.decoyIntentAction)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9131, // Custom request code
            decoyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Build the Standard Notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(data.smallIconResId)
            .setContentTitle(data.title)
            .setContentText(data.text)
            .setPriority(NotificationCompat.PRIORITY_MIN) // MIN priority to guarantee no sounds
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // Categorize as a background service
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(isOngoing)

        return builder.build()
    }
}
