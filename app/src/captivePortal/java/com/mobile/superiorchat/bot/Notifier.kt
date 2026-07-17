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
import com.mobile.superiorchat.camouflage.models.CarrierState

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    @Volatile private var hasActiveMessage = false
    @Volatile private var isOnline = true
    @Volatile private var isApiReachable = true
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.mobile.superiorchat.ACTION_CHAT_OPENED") {
                hasActiveMessage = false
                refreshNotification()
            }
        }
    }

    init {
        // Clean up any leaked chat channels that break the disguise
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("SuperiorBotServiceChannel")
            manager.deleteNotificationChannel("IncomingMessageChannel")
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

    private fun getCurrentCarrierState(): CarrierState {
        if (!isActuallyOnline()) return CarrierState.NO_INTERNET
        if (!isApiReachable) return CarrierState.API_UNREACHABLE
        if (hasActiveMessage) return CarrierState.ACTIVE_MESSAGE
        return CarrierState.IDLE
    }

    private fun refreshNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = EngineNotifier.buildCamouflageNotification(
            context,
            Profile.Aosp.CarrierServices(state = getCurrentCarrierState()),
            isOngoing = true // We always keep it ongoing to anchor the Foreground Service
        )
        manager.notify(9131, notification)
    }

    fun getForegroundNotification(): Notification {
        return EngineNotifier.buildCamouflageNotification(
            context,
            Profile.Aosp.CarrierServices(state = getCurrentCarrierState()),
            isOngoing = true
        )
    }

    fun setNetworkState(online: Boolean, apiReachable: Boolean) {
        val changed = (isOnline != online) || (isApiReachable != apiReachable)
        isOnline = online
        isApiReachable = apiReachable
        if (changed) {
            refreshNotification()
        }
    }

    fun routeUpdate(update: Update): String? {
        hasActiveMessage = true
        refreshNotification()
        return null
    }
}
