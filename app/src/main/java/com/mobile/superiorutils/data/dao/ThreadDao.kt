package com.mobile.superiorutils.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<com.mobile.superiorutils.data.entity.ChatNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConversation(conversation: com.mobile.superiorutils.data.entity.ChatNode): Long
}
