package com.mobile.superiorchat.bot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GetMeResponse(
    val ok: Boolean,
    val result: User? = null
)

@Serializable
data class UpdateResponse(
    val ok: Boolean,
    val result: List<Update> = emptyList()
)


@Serializable
data class Update(
    val update_id: Long,
    val message: Message? = null,
    val edited_message: Message? = null
)

@Serializable
data class Message(
    val message_id: Long,
    val from: User? = null,
    val chat: Chat,
    val date: Long = 0,
    val text: String? = null,
    val photo: List<JsonElement>? = null,
    val document: JsonElement? = null,
    val video: JsonElement? = null,
    val audio: JsonElement? = null,
    val voice: JsonElement? = null,
    val reply_to_message: Message? = null,
    val pinned_message: Message? = null
)


@Serializable
data class User(
    val id: Long,
    val is_bot: Boolean = false,
    val first_name: String,
    val username: String? = null
)

@Serializable
data class Chat(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null,
    val first_name: String? = null,
    val photo: ChatPhoto? = null,
    val bio: String? = null,
    val description: String? = null,
    val invite_link: String? = null,
    val has_protected_content: Boolean? = null,
    val is_forum: Boolean? = null,
    val pinned_message: Message? = null
)

@Serializable
data class ChatPhoto(
    val small_file_id: String,
    val small_file_unique_id: String,
    val big_file_id: String,
    val big_file_unique_id: String
)

@Serializable
data class ChatResponse(
    val ok: Boolean,
    val result: Chat? = null
)

@Serializable
data class TelegramFile(
    val file_id: String,
    val file_unique_id: String,
    val file_size: Long? = null,
    val file_path: String? = null
)

@Serializable
data class FileResponse(
    val ok: Boolean,
    val result: TelegramFile? = null
)
