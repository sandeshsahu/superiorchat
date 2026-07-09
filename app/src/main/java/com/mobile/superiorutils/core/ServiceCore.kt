package com.mobile.superiorutils.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mobile.superiorutils.service.BotService
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogLevel
import com.mobile.superiorutils.utils.AppLog

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
            AppLog.log(LogCategory.SYSTEM, "ServiceCore: Failed to start service: ${e.message}", LogLevel.ERROR)
        }
    }
}
