package com.mobile.superiorutils.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.mobile.superiorutils.MainActivity
import com.mobile.superiorutils.utils.LogLevel
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.AppLog

class CodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_SECRET_CODE) {
            return
        }

        val code = intent.data?.schemeSpecificPart ?: "Unknown"
        AppLog.log(LogCategory.SYSTEM, "Secret dialer code triggered (*#*#$code#*#*). Launching MainActivity.")

        val i = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("isSecretLaunch", true)
        }
        
        try {
            context.startActivity(i)
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "Failed to launch MainActivity from dialer: ${e.message}", LogLevel.ERROR)
        }
    }
}
