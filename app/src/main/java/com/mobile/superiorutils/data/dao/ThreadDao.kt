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

    @Query("SELECT * FROM conversations WHERE chatId = :chatId")
    fun getConversation(chatId: String): Flow<com.mobile.superiorutils.data.entity.ChatNode?>

    @Query("SELECT * FROM conversations WHERE chatId = :chatId")
    suspend fun getConversationSync(chatId: String): com.mobile.superiorutils.data.entity.ChatNode?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversation(conversation: com.mobile.superiorutils.data.entity.ChatNode): Long

    @androidx.room.Update
    suspend fun updateConversation(conversation: com.mobile.superiorutils.data.entity.ChatNode): Int
}
