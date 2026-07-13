package com.mobile.superiorutils.service

import com.mobile.superiorutils.bot.BotSync
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobile.superiorutils.core.AppGraph
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogLevel
import kotlinx.coroutines.delay

class BotWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppLog.log(LogCategory.SYSTEM, "BotWorker: Started as fallback for BotService.")
        val prefs = AppGraph.prefs
        if (!prefs.isConfigured) {
            return Result.success()
        }

        val botSync = BotSync(applicationContext)
        try {
            botSync.startPolling()
            // Keep the worker alive for 9 minutes (WorkManager max is 10 mins)
            delay(9 * 60 * 1000L)
        } catch (e: Exception) {
            AppLog.log(LogCategory.SYSTEM, "BotWorker: Failed: ${e.message}", LogLevel.ERROR)
            return Result.retry()
        } finally {
            botSync.stopPolling()
            AppLog.log(LogCategory.SYSTEM, "BotWorker: Finished fallback execution.")
        }

        return Result.success()
    }
}
