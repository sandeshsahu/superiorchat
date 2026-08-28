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
    val pinnedMessageId: Long? = null,
    val pinnedMessageIds: String? = null,

    // Chat Attributes
    val chatType: String? = null,
    val description: String? = null,
    val photoPath: String? = null,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val memberCount: Int = 0,

    // Local App State
    val draftText: String? = null,
    val draftTimestamp: Long? = null,
    val ttlSeconds: Int = 0,
    val wallpaperPath: String? = null
)
