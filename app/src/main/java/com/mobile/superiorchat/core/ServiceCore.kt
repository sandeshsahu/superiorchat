package com.mobile.superiorchat.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mobile.superiorchat.service.BotService
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import com.mobile.superiorchat.utils.AppLog

object ServiceCore {

    fun ensureRunning(context: Context) {
        if (!AppGraph.prefs.isConfigured) {
            AppLog.log(LogCategory.SYSTEM, "ServiceCore: Credentials not configured, skipping start")
            return
        }
        
        try {
            val serviceIntent = Intent(context, BotService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            AppLog.log(LogCategory.SYSTEM, "ServiceCore: Started BotService")
        } catch (e: Exception) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && 
                e is android.app.ForegroundServiceStartNotAllowedException) {
                AppLog.log(LogCategory.SYSTEM, "ServiceCore: ForegroundService blocked (Android 12+). Falling back to WorkManager.", LogLevel.WARN)
                try {
                    val request = androidx.work.OneTimeWorkRequestBuilder<com.mobile.superiorchat.service.BotWorker>().build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "BotWorkerFallback",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        request
                    )
                } catch (we: Exception) {
                    AppLog.log(LogCategory.SYSTEM, "ServiceCore: WorkManager fallback failed: ${we.message}", LogLevel.ERROR)
                }
            } else {
                AppLog.log(LogCategory.SYSTEM, "ServiceCore: Failed to start service: ${e.message}", LogLevel.ERROR)
            }
        }
    }
}
