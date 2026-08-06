package com.mobile.superiorchat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.superiorchat.data.entity.CallHistoryNode
import kotlinx.coroutines.flow.Flow

@Dao
interface CallHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallHistoryNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(calls: List<CallHistoryNode>): List<Long>

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallHistoryNode>>

    @Query("DELETE FROM call_history")
    suspend fun clearHistory(): Int

    @androidx.room.Delete
    suspend fun deleteCall(call: CallHistoryNode): Int
}
