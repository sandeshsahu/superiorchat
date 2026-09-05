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
import com.mobile.superiorchat.utils.Validator

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    private val MESSAGE_NOTIFICATION_ID = 1001
    private val CALL_NOTIFICATION_ID = 9132
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

        val callDeclineReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "com.mobile.superiorchat.ACTION_DECLINE_CALL") {
                    com.mobile.superiorchat.core.call.CallManager.declineIncomingCall()
                    cancelIncomingCallNotification()
                }
            }
        }
        val callFilter = IntentFilter("com.mobile.superiorchat.ACTION_DECLINE_CALL")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(callDeclineReceiver, callFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(callDeclineReceiver, callFilter)
        }

        com.mobile.superiorchat.core.call.CallManager.onIncomingCallRingingListener = { callerName ->
            showIncomingCallNotification(callerName)
        }
        com.mobile.superiorchat.core.call.CallManager.onStopRingingListener = {
            cancelIncomingCallNotification()
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
            .setSmallIcon(com.mobile.superiorchat.R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    fun routeUpdate(update: Update): String? {
        // If the user is actively inside the chat app looking at the screen, do not show OS notifications
        if (com.mobile.superiorchat.core.AppGraph.isChatInForeground) {
            return null
        }

        val message = update.message ?: return null
        if (Validator.extractSystemSignal(message.text ?: message.caption) !is Validator.SystemSignal.None) {
            return null
        }
        val text = Validator.formatNotificationText(message)
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
            val senderIdentifier = message.from?.username?.takeIf { it.isNotBlank() } ?: message.from?.id?.toString()
            val profile = try {
                AppGraph.appRepository.resolveActivePartnerProfile(senderIdentifier)
            } catch (e: Exception) { null }

            val senderName = profile?.title ?: message.from?.first_name ?: "Unknown"
            
            val personBuilder = Person.Builder().setName(senderName)
            val photoPath = profile?.profilePhotoPath
            if (!photoPath.isNullOrBlank()) {
                try {
                    val file = java.io.File(photoPath)
                    if (file.exists() && file.length() > 0) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            personBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
                        }
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
                .setSmallIcon(com.mobile.superiorchat.R.drawable.ic_notification)
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

    fun showIncomingCallNotification(callerName: String) {
        // If the user is actively inside the chat app, the in-app IncomingCallDialog handles it
        if (com.mobile.superiorchat.core.AppGraph.isChatInForeground) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "IncomingCallChannel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming voice and video call notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val targetClass = if (com.mobile.superiorchat.core.AppGraph.prefs.isFakeCrashEnabled) {
            com.mobile.superiorchat.TransparentActivity::class.java
        } else {
            com.mobile.superiorchat.MainActivity::class.java
        }

        // Tapping the notification body opens the app to the incoming call dialog (does NOT auto-accept)
        val openAppIntent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tapping the explicit Accept action triggers the acceptance + validation flow
        val acceptIntent = Intent(context, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ACTION_ACCEPT_INCOMING_CALL", true)
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            102,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent("com.mobile.superiorchat.ACTION_DECLINE_CALL").apply {
            setPackage(context.packageName)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            103,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scope.launch {
            val profile = try {
                AppGraph.appRepository.resolveActivePartnerProfile(callerName)
            } catch (e: Exception) { null }

            val displayName = callerName.ifBlank { profile?.title ?: "Partner" }
            val personBuilder = Person.Builder().setName(displayName).setImportant(true)

            val photoPath = profile?.profilePhotoPath
            if (!photoPath.isNullOrBlank()) {
                try {
                    val file = java.io.File(photoPath)
                    if (file.exists() && file.length() > 0) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            personBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
                        }
                    }
                } catch (e: Exception) {
                    com.mobile.superiorchat.utils.AppLog.log(
                        com.mobile.superiorchat.utils.LogCategory.SYSTEM,
                        "Failed to decode profile photo: ${e.message}",
                        com.mobile.superiorchat.utils.LogLevel.WARN
                    )
                }
            }
            val callerPerson = personBuilder.build()

            try {
                val callStyle = NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson,
                    declinePendingIntent,
                    acceptPendingIntent
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.mobile.superiorchat.R.drawable.ic_notification)
                    .setStyle(callStyle)
                    .setColor(0xFF00E676.toInt()) // Vibrant Green Call Accent
                    .setColorized(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(openAppPendingIntent)
                    .setFullScreenIntent(openAppPendingIntent, true)
                    .build()

                notificationManager.notify(CALL_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                com.mobile.superiorchat.utils.AppLog.log(
                    com.mobile.superiorchat.utils.LogCategory.SYSTEM,
                    "CallStyle notification failed: ${e.message}, falling back to standard notification",
                    com.mobile.superiorchat.utils.LogLevel.WARN
                )
                val fallbackNotification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.mobile.superiorchat.R.drawable.ic_notification)
                    .setContentTitle("Incoming Call")
                    .setContentText(if (displayName.isNotBlank()) "$displayName is calling..." else "Incoming call...")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setColor(0xFF00E676.toInt())
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(openAppPendingIntent)
                    .setFullScreenIntent(openAppPendingIntent, true)
                    .addAction(com.mobile.superiorchat.R.drawable.ic_call_end, "Decline", declinePendingIntent)
                    .addAction(com.mobile.superiorchat.R.drawable.ic_notification, "Accept", acceptPendingIntent)
                    .build()

                notificationManager.notify(CALL_NOTIFICATION_ID, fallbackNotification)
            }
        }
    }

    fun cancelIncomingCallNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(CALL_NOTIFICATION_ID)
    }
}
