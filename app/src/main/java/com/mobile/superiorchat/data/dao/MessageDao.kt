package com.mobile.superiorchat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM (SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String, limit: Int): Flow<List<MessageNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageNode>): List<Long>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus): Int

    @Query("UPDATE messages SET messageId = :newId, status = :status WHERE messageId = :oldId")
    suspend fun updateMessageIdAndStatus(oldId: Long, newId: Long, status: MessageStatus): Int

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): MessageNode?

    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: Long, newText: String): Int

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: Long): Int

    @Query("UPDATE messages SET reactions = :reactions WHERE messageId = :messageId")
    suspend fun updateMessageReactions(messageId: Long, reactions: String?): Int

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getMessagesByStatus(status: MessageStatus): List<MessageNode>

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages(): Int
}
