package com.mobile.superiorutils.utils

import com.mobile.superiorutils.utils.LogLevel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mobile.superiorutils.core.ServiceCore
import com.mobile.superiorutils.core.AppGraph
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog

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
