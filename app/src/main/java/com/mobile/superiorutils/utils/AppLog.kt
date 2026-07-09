package com.mobile.superiorutils.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.ArrayDeque

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

enum class LogCategory(val displayName: String) {
    SYSTEM("System"),
    BOT_ACTIVITY("Bot Activity"),
    NETWORK("Network"),
    ERROR("Errors")
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val category: LogCategory,
    val message: String
)

object AppLog {
    private const val MAX_LOGS_PER_CATEGORY = 150
    private const val TAG = "SuperiorChat"

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }

    private val _isTelegramApiReachable = MutableStateFlow(false)
    val isTelegramApiReachable: StateFlow<Boolean> = _isTelegramApiReachable

    fun setTelegramApiReachable(isReachable: Boolean) {
        _isTelegramApiReachable.value = isReachable
    }

    private val logQueues = LogCategory.entries.associateWith { ArrayDeque<LogEntry>() }
    private val _logFlows = LogCategory.entries.associateWith { MutableStateFlow<List<LogEntry>>(emptyList()) }

    // Combined flow for "All" tab
    private val _allLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val allLogs: StateFlow<List<LogEntry>> = _allLogs

    fun getLogs(category: LogCategory): StateFlow<List<LogEntry>> = _logFlows[category]!!

    fun log(category: LogCategory, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(System.currentTimeMillis(), level, category, message)

        synchronized(logQueues) {
            val queue = logQueues[category]!!
            if (queue.size >= MAX_LOGS_PER_CATEGORY) {
                queue.removeFirst()
            }
            queue.addLast(entry)

            _logFlows[category]!!.value = queue.toList()

            // Mirror error logs to the ERROR category queue
            if (level == LogLevel.ERROR && category != LogCategory.ERROR) {
                val errQueue = logQueues[LogCategory.ERROR]!!
                if (errQueue.size >= MAX_LOGS_PER_CATEGORY) errQueue.removeFirst()
                errQueue.addLast(entry)
                _logFlows[LogCategory.ERROR]!!.value = errQueue.toList()
            }

            // Optimized rebuild of "All" logs list: prepend and prune instead of flat-mapping/sorting everything
            val currentAll = _allLogs.value
            val newAll = ArrayList<LogEntry>(currentAll.size + 1).apply {
                add(entry)
                addAll(currentAll.filter { it.message != entry.message || Math.abs(it.timestamp - entry.timestamp) > 10 })
            }
            _allLogs.value = if (newAll.size > MAX_LOGS_PER_CATEGORY) {
                newAll.subList(0, MAX_LOGS_PER_CATEGORY)
            } else {
                newAll
            }
        }

        // Also log to Logcat for debugging
        when (level) {
            LogLevel.ERROR -> android.util.Log.e(TAG, "[${category.name}] $message")
            LogLevel.WARN -> android.util.Log.w(TAG, "[${category.name}] $message")
            LogLevel.DEBUG -> android.util.Log.d(TAG, "[${category.name}] $message")
            LogLevel.INFO -> android.util.Log.i(TAG, "[${category.name}] $message")
        }
    }

    fun clearLogs(category: LogCategory) {
        synchronized(logQueues) {
            logQueues[category]!!.clear()
            _logFlows[category]!!.value = emptyList()
            rebuildAllLogs()
        }
    }

    fun clearAllLogs() {
        synchronized(logQueues) {
            logQueues.values.forEach { it.clear() }
            _logFlows.values.forEach { it.value = emptyList() }
            _allLogs.value = emptyList()
        }
    }

    private fun rebuildAllLogs() {
        _allLogs.value = logQueues.values
            .flatMap { it.toList() }
            .distinctBy { it.timestamp.toString() + it.message.hashCode() }
            .sortedByDescending { it.timestamp }
            .take(MAX_LOGS_PER_CATEGORY)
    }
}
