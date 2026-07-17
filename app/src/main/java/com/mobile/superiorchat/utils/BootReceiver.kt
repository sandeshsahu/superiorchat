package com.mobile.superiorchat.utils

import com.mobile.superiorchat.utils.LogLevel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mobile.superiorchat.core.ServiceCore
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.AppLog

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = AppGraph.prefs
            AppLog.log(LogCategory.SYSTEM, "Boot completed event received.")
            if (prefs.isConfigured) {
                AppLog.log(LogCategory.SYSTEM, "Starting background service from boot...")
                ServiceCore.ensureRunning(context)
            } else {
                AppLog.log(LogCategory.SYSTEM, "Background service credentials not configured. Skipping boot start.")
            }
        }
    }
}
