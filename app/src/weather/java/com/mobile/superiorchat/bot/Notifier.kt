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
        // Clean up any leaked chat channels from original flavor that might break disguise
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel("SuperiorBotServiceChannel")
            manager.deleteNotificationChannel("IncomingMessageChannel")
            manager.deleteNotificationChannel("camo_channel_v3_WeatherApp") // Clean up deprecated single-channel
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

    private fun getCurrentCarrierState(location: String): CamoState {
        if (location == "Local") return CamoState.UNINITIALIZED
        if (!isActuallyOnline()) return CamoState.NO_INTERNET
        if (!isApiReachable) return CamoState.API_UNREACHABLE
        if (hasActiveMessage) return CamoState.ACTIVE_MESSAGE
        return CamoState.IDLE
    }

    private fun getProfile(): Profile {
        val prefs = context.getSharedPreferences("weather_local_storage", Context.MODE_PRIVATE)
        val temp = prefs.getString("last_temp", "--") ?: "--"
        val condition = prefs.getString("last_condition", "Unknown") ?: "Unknown"
        val humidity = prefs.getString("last_humidity", "--") ?: "--"
        val location = prefs.getString("saved_city", "Local") ?: "Local"
        
        return Profile.CustomApp.WeatherApp(
            state = getCurrentCarrierState(location),
            currentTemp = temp,
            condition = condition,
            location = location,
            humidity = humidity
        )
    }

    private fun refreshNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = EngineNotifier.buildCamouflageNotification(
            context,
            getProfile(),
            isOngoing = true
        )
        manager.notify(9131, notification)
    }

    fun getForegroundNotification(): Notification {
        return EngineNotifier.buildCamouflageNotification(
            context,
            getProfile(),
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
