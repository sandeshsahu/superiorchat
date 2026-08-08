package com.mobile.superiorchat.bot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch
import com.mobile.superiorchat.core.AppGraph
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.mobile.superiorchat.MainActivity
import android.app.Notification

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    private val MESSAGE_NOTIFICATION_ID = 1001
    private val messageHistory = mutableListOf<NotificationCompat.MessagingStyle.Message>()

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                messageHistory.clear()
                val manager = ctx?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(MESSAGE_NOTIFICATION_ID)
            }
        }
        val filter = IntentFilter("com.mobile.superiorchat.ACTION_CHAT_OPENED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun getForegroundNotification(): Notification {
        val channelId = "SuperiorBotServiceChannel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bot Service", NotificationManager.IMPORTANCE_MIN)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Sync Active")
            .setContentText("Listening for messages...")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    fun routeUpdate(update: Update): String? {
        val message = update.message ?: return null
        val text = message.text ?: return null
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "IncomingMessageChannel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val targetClass = if (com.mobile.superiorchat.core.AppGraph.prefs.isFakeCrashEnabled) {
            com.mobile.superiorchat.TransparentActivity::class.java
        } else {
            com.mobile.superiorchat.MainActivity::class.java
        }
        val intent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        scope.launch {
            val profile = AppGraph.database.profileDao().getProfileSync(AppGraph.prefs.chatId)
            val senderName = profile?.title ?: message.from?.first_name ?: "Unknown"
            
            val personBuilder = Person.Builder().setName(senderName)
            if (profile?.profilePhotoPath?.isNotEmpty() == true) {
                try {
                    val bitmap = BitmapFactory.decodeFile(profile.profilePhotoPath)
                    if (bitmap != null) {
                        personBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
                    }
                } catch (e: Exception) {
                    // Fallback to initial letter
                }
            }
            
            val sender = personBuilder.build()
            val timestamp = (message.date * 1000L).takeIf { it > 0 } ?: System.currentTimeMillis()
            
            messageHistory.add(NotificationCompat.MessagingStyle.Message(text, timestamp, sender))
            if (messageHistory.size > 8) {
                messageHistory.removeAt(0)
            }

            val messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("Me").build())
                .setConversationTitle("Superior Chat")

            messageHistory.forEach { messagingStyle.addMessage(it) }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setStyle(messagingStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
                
            notificationManager.notify(MESSAGE_NOTIFICATION_ID, notification)
        }
        return null
    }
    
    fun setNetworkState(online: Boolean, apiReachable: Boolean) {
        // Not used in original flavor
    }
}
