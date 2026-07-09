package com.mobile.superiorutils.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageNode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageNode>): List<Long>

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    fun updateMessageStatus(messageId: Long, status: MessageStatus): Int

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    fun getMessageById(messageId: Long): MessageNode?

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp ASC")
    fun getMessagesByStatus(status: MessageStatus): List<MessageNode>
}
