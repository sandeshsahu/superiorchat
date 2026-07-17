package com.mobile.superiorchat.service

import com.mobile.superiorchat.bot.BotSync
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
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
