package com.mobile.superiorutils.bot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

import kotlinx.coroutines.CoroutineScope

class Notifier(private val context: Context, private val scope: CoroutineScope) {


    fun routeUpdate(update: Update): String? {
        val message = update.message ?: return null
        val text = message.text ?: return null
        
        // Show a basic notification for incoming message
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "IncomingMessageChannel"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("New message from ${message.from?.first_name ?: "Unknown"}")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(message.message_id.toInt(), notification)

        return null // Don't auto-reply, let the user reply from the app
    }
}
