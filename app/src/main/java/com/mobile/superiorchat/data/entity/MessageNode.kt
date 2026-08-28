package com.mobile.superiorchat.data.entity

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
    val editTimestamp: Long? = null,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENDING,

    // Sender Metadata
    val isFromBot: Boolean = false,
    val senderUsername: String? = null,
    val senderName: String? = null,
    val senderPhotoPath: String? = null,
    val senderRole: String? = null,
    val chatType: String? = null,

    // Rich Media & Telemetry
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val mediaLocalPath: String? = null,
    val mediaFileName: String? = null,
    val mediaFileSize: Long? = null,
    val mediaMimeType: String? = null,
    val mediaDuration: Int? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val mediaThumbPath: String? = null,
    val mediaWaveform: String? = null,

    // Entities & Formatting
    val entities: String? = null,

    // Quotes & Replies
    val replyToMessageId: Long? = null,
    val replyToText: String? = null,
    val replyToAuthor: String? = null,

    // Forwarding
    val forwardFromId: String? = null,
    val forwardFromName: String? = null,
    val forwardDate: Long? = null,

    // State & Security
    val isEdited: Boolean = false,
    val isStarred: Boolean = false,
    val expiresAt: Long? = null,
    // JSON string representing ReactionData. Null means no reactions.
    val reactions: String? = null
)

@kotlinx.serialization.Serializable
data class ReactionData(
    val me: List<String> = emptyList(),
    val peer: List<String> = emptyList()
) {
    fun allReactions(): Set<String> = (me + peer).toSet()

    companion object {
        private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        fun parse(jsonString: String?): ReactionData {
            if (jsonString.isNullOrBlank()) return ReactionData()
            return try {
                if (jsonString.startsWith("{")) {
                    json.decodeFromString(jsonString)
                } else {
                    // Legacy fallback: assuming old comma-separated emojis came from peer
                    val emojis = jsonString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    ReactionData(peer = emojis)
                }
            } catch (e: Exception) {
                ReactionData()
            }
        }

        fun toJson(data: ReactionData): String? {
            if (data.me.isEmpty() && data.peer.isEmpty()) return null
            return json.encodeToString(ReactionData.serializer(), data)
        }
    }
}
