package com.mobile.superiorutils.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ChatNode::class,
            parentColumns = ["chatId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageNode(
    @PrimaryKey(autoGenerate = false)
    val messageId: Long,
    val conversationId: String,
    val senderId: String,
    val text: String?,
    val timestamp: Long,
    val isFromMe: Boolean,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val mediaLocalPath: String? = null,
    val status: MessageStatus = MessageStatus.SENDING,
    val mediaFileName: String? = null,
    val mediaFileSize: Long? = null,
    val replyToMessageId: Long? = null,
    val isEdited: Boolean = false
)
