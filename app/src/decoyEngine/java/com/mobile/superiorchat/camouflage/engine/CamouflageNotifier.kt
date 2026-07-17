package com.mobile.superiorchat.camouflage.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.CamouflageProfile
import com.mobile.superiorchat.camouflage.ui.DecoyActivity

object CamouflageNotifier {

    fun showTestCamouflage(context: Context, profile: CamouflageProfile) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val data = CamouflageManager.resolveCamouflage(profile)
        
        val channelId = "camo_channel_v2_${profile.javaClass.simpleName}"
        
        // 1. Create Spoofed Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                data.appNameSpoof, // This overrides the App Name on newer Androids in Settings
                if (data.isSilent) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System notifications"
                setShowBadge(false)
            }
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

        // 3. Build Custom RemoteViews
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification_camo).apply {
            setTextViewText(R.id.camo_header, data.appNameSpoof)
            setTextViewText(R.id.camo_title, data.title)
            setTextViewText(R.id.camo_text, data.text)
            setImageViewResource(R.id.camo_icon, data.smallIconResId)
        }

        // 4. Build the Notification with Custom Layout
        val builder = NotificationCompat.Builder(context, channelId)
            // Small icon is strictly required by the OS to not crash, but it won't be visible in the main drawer
            .setSmallIcon(data.smallIconResId) 
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews) // Ensures it stays custom when expanded
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        manager.notify(9131, builder.build())
    }
}
