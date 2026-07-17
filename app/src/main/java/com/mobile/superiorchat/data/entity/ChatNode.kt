package com.mobile.superiorchat.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ChatNode(
    @PrimaryKey
    val chatId: String,
    val title: String,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val pinnedMessageId: Long? = null
)
