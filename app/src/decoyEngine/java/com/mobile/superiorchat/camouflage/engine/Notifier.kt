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

    fun buildCamouflageNotification(
        context: Context, 
        profile: Profile, 
        isOngoing: Boolean = false,
        forceHeadsUp: Boolean = false
    ): Notification {
        val data = Manager.resolveCamouflage(context, profile)
        
        val channelIdSuffix = if (data.isSilent) "silent" else "alert"
        val channelId = "camo_channel_v4_${profile.javaClass.simpleName}_$channelIdSuffix"
        
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

        val priority = when {
            data.isSilent -> NotificationCompat.PRIORITY_MIN
            profile.state == com.mobile.superiorchat.camouflage.models.CamoState.ACTIVE_CALL -> NotificationCompat.PRIORITY_MAX
            else -> NotificationCompat.PRIORITY_HIGH
        }

        val category = when (profile.state) {
            com.mobile.superiorchat.camouflage.models.CamoState.ACTIVE_CALL -> NotificationCompat.CATEGORY_CALL
            com.mobile.superiorchat.camouflage.models.CamoState.ACTIVE_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
            else -> NotificationCompat.CATEGORY_SERVICE
        }

        // 3. Build the Standard Notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(data.smallIconResId)
            .setContentTitle(data.title)
            .setContentText(data.text)
            .setPriority(priority)
            .setCategory(category)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(isOngoing)
            .setOnlyAlertOnce(!forceHeadsUp)

        if (forceHeadsUp) {
            builder.setWhen(System.currentTimeMillis())
        }

        return builder.build()
    }

    /**
     * Subtle, discreet tactile pulse for incoming camouflage messages.
     * Complies strictly with Android system ringer modes (SILENT = 0 vibration).
     */
    fun triggerDiscreetMessageVibration(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val ringerMode = am?.ringerMode ?: android.media.AudioManager.RINGER_MODE_NORMAL
            if (ringerMode == android.media.AudioManager.RINGER_MODE_SILENT) return

            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            val pattern = longArrayOf(0, 100, 80, 100)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 60, 0, 60)
                vib?.vibrate(android.os.VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {}
    }
}
