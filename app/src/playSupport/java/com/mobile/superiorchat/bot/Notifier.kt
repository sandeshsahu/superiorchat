package com.mobile.superiorchat.bot

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import com.mobile.superiorchat.camouflage.engine.Notifier as EngineNotifier
import com.mobile.superiorchat.camouflage.models.Profile
import com.mobile.superiorchat.camouflage.models.CamoState

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    @Volatile private var messageAlertCount = 0
    @Volatile private var callAlertCount = 0
    @Volatile private var hasActiveMessage = false
    @Volatile private var hasActiveCall = false
    @Volatile private var isOnline = true
    @Volatile private var isApiReachable = true
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.mobile.superiorchat.ACTION_CHAT_OPENED") {
                messageAlertCount = 0
                callAlertCount = 0
                hasActiveMessage = false
                hasActiveCall = false
                refreshNotification(forceHeadsUp = false)
            }
        }
    }

    init {
        // Clean up any leaked chat channels that break the disguise
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("SuperiorBotServiceChannel")
            manager.deleteNotificationChannel("IncomingMessageChannel")
            manager.deleteNotificationChannel("IncomingCallChannel")
            manager.deleteNotificationChannel("camo_channel_v3_PlaySupport")
        }

        // Register incoming call ringing listeners for camouflage alert
        com.mobile.superiorchat.core.call.CallManager.onIncomingCallRingingListener = {
            showIncomingCallNotification()
        }
        com.mobile.superiorchat.core.call.CallManager.onStopRingingListener = {
            cancelIncomingCallNotification()
        }
        
        // Register receiver for auto-clear
        val filter = IntentFilter("com.mobile.superiorchat.ACTION_CHAT_OPENED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun isActuallyOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getCurrentPlayState(): CamoState {
        if (!isActuallyOnline()) return CamoState.NO_INTERNET
        if (!isApiReachable) return CamoState.API_UNREACHABLE
        if (hasActiveCall) return CamoState.ACTIVE_CALL
        if (hasActiveMessage) return CamoState.ACTIVE_MESSAGE
        return CamoState.IDLE
    }

    private fun refreshNotification(forceHeadsUp: Boolean = false) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = EngineNotifier.buildCamouflageNotification(
            context = context,
            profile = Profile.Aosp.PlaySupport(state = getCurrentPlayState()),
            isOngoing = true,
            forceHeadsUp = forceHeadsUp
        )
        manager.notify(9131, notification)
    }

    fun getForegroundNotification(): Notification {
        return EngineNotifier.buildCamouflageNotification(
            context = context,
            profile = Profile.Aosp.PlaySupport(state = getCurrentPlayState()),
            isOngoing = true,
            forceHeadsUp = false
        )
    }

    fun setNetworkState(online: Boolean, apiReachable: Boolean) {
        val changed = (isOnline != online) || (isApiReachable != apiReachable)
        isOnline = online
        isApiReachable = apiReachable
        if (changed) {
            refreshNotification(forceHeadsUp = false)
        }
    }

    fun routeUpdate(update: Update): String? {
        // If the user is actively inside the chat app looking at the screen, do not buzz or alert
        if (com.mobile.superiorchat.core.AppGraph.isChatInForeground) {
            return null
        }

        hasActiveMessage = true
        if (messageAlertCount < 2) {
            messageAlertCount++
            refreshNotification(forceHeadsUp = true)
            EngineNotifier.triggerDiscreetMessageVibration(context)
        } else {
            refreshNotification(forceHeadsUp = false)
        }
        return null
    }

    fun showIncomingCallNotification() {
        // If the user is actively inside the chat app looking at the screen, the in-app IncomingCallDialog handles it
        if (com.mobile.superiorchat.core.AppGraph.isChatInForeground) {
            return
        }

        hasActiveCall = true
        if (callAlertCount < 2) {
            callAlertCount++
            refreshNotification(forceHeadsUp = true)
        } else {
            // Cap call heads-up and vibration if caller keeps spamming calls without user opening chat
            refreshNotification(forceHeadsUp = false)
            com.mobile.superiorchat.core.call.CallManager.stopRinging()
        }
    }

    fun cancelIncomingCallNotification() {
        hasActiveCall = false
        refreshNotification(forceHeadsUp = false)
    }
}
