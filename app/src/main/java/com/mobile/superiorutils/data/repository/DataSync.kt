package com.mobile.superiorutils.data.repository

import com.mobile.superiorutils.data.dao.ThreadDao
import com.mobile.superiorutils.data.dao.MessageDao
import com.mobile.superiorutils.data.entity.ChatNode
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

class DataSync(
    private val conversationDao: ThreadDao,
    private val messageDao: MessageDao
) {
    fun getAllConversations(): Flow<List<ChatNode>> {
        return conversationDao.getAllConversations()
    }

    fun getMessagesForConversation(chatId: String, limit: Int): Flow<List<MessageNode>> {
        return messageDao.getMessagesForConversation(chatId, limit)
    }

    suspend fun insertMessage(message: MessageNode) {
        messageDao.insertMessage(message)
    }

    suspend fun insertOrUpdateConversation(conversation: ChatNode) {
        val id = conversationDao.insertConversation(conversation)
        if (id == -1L) {
            conversationDao.updateConversation(conversation)
        }
    }

    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus) {
        messageDao.updateMessageStatus(messageId, status)
     }

    suspend fun getQueuedMessages(): List<MessageNode> {
        return messageDao.getMessagesByStatus(MessageStatus.QUEUED)
    }
}
