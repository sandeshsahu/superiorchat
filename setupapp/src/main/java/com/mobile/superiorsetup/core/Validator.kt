package com.mobile.superiorsetup.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TelegramBotUser(
    val id: Long,
    val is_bot: Boolean = true,
    val first_name: String? = null,
    val username: String? = null,
    val can_read_all_group_messages: Boolean? = null
)

object Validator {
    private val BOT_TOKEN_REGEX = Regex("^\\d{8,10}:[A-Za-z0-9_-]{35}\$")
    private val CHAT_ID_REGEX = Regex("^-?\\d{7,15}\$")
    private val GROUP_CHAT_ID_REGEX = Regex("^-(?:100\\d{9,13}|\\d{6,12})\$")
    private val WEBRTC_URL_REGEX = Regex("^https?://([a-zA-Z0-9.-]+)(:\\d+)?/?\$")
    private val PARTNER_USERNAME_REGEX = Regex("^@[a-zA-Z0-9_]{3,32}\$")
    private val ADMIN_PARTNER_BOT_USERNAME_REGEX = Regex("""^@[a-zA-Z0-9_]{1,29}(?i:bot)$""")

    sealed class ValidationResult<out T> {
        data class Success<T>(val data: T) : ValidationResult<T>()
        data class Error(val message: String) : ValidationResult<Nothing>()
        data class NetworkError(val message: String) : ValidationResult<Nothing>()
    }

    sealed class BotGroupMemberStatus {
        data class Success(val groupTitle: String, val groupType: String) : BotGroupMemberStatus()
        data class NotGroup(val actualType: String) : BotGroupMemberStatus()
        data class Migrated(val newChatId: String, val groupTitle: String) : BotGroupMemberStatus()
        object NotInGroup : BotGroupMemberStatus()
        object Restricted : BotGroupMemberStatus()
        data class NotAdmin(val currentStatus: String) : BotGroupMemberStatus()
        object GroupPrivacyEnabled : BotGroupMemberStatus()
        data class ApiError(val message: String) : BotGroupMemberStatus()
        data class NetworkError(val message: String) : BotGroupMemberStatus()
    }

    fun isValidBotToken(token: String): Boolean {
        return BOT_TOKEN_REGEX.matches(token.trim())
    }

    fun isValidChatId(chatId: String): Boolean {
        return CHAT_ID_REGEX.matches(chatId.trim())
    }

    fun isValidGroupChatId(chatId: String): Boolean {
        return GROUP_CHAT_ID_REGEX.matches(chatId.trim())
    }

    fun isValidWebRtcUrl(url: String): Boolean {
        if (url.isEmpty()) return true
        return WEBRTC_URL_REGEX.matches(url.trim())
    }

    fun isValidPartnerBotUsername(username: String, isAdminMode: Boolean = false): Boolean {
        if (username.isBlank()) return true
        return if (isAdminMode) {
            ADMIN_PARTNER_BOT_USERNAME_REGEX.matches(username.trim())
        } else {
            PARTNER_USERNAME_REGEX.matches(username.trim())
        }
    }

    private fun executeGet(urlStr: String): Pair<Int?, String?> {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 9000
            conn.readTimeout = 9000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "SuperiorChat-SetupApp/1.0")
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }
            Pair(code, text)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    /**
     * Live validator for Telegram Bot Token.
     * Hits Telegram getMe endpoint asynchronously to verify token validity and retrieve bot info.
     */
    suspend fun verifyBotToken(token: String): ValidationResult<TelegramBotUser> = withContext(Dispatchers.IO) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return@withContext ValidationResult.Error("Please enter your Telegram Bot Token.")
        if (!isValidBotToken(trimmed)) return@withContext ValidationResult.Error("Invalid Bot Token format. It should look like 1234567890:AAH...")

        try {
            val urlStr = "https://api.telegram.org/bot$trimmed/getMe"
            val (code, body) = executeGet(urlStr)
            if (code == null || body == null) {
                return@withContext ValidationResult.NetworkError("Cannot connect to Telegram servers. Please check your internet connection and try again.")
            }
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false) || !json.has("result")) {
                val desc = json.optString("description", "Invalid bot token or token has been revoked by @BotFather.")
                return@withContext ValidationResult.Error("Invalid bot token: $desc")
            }
            val result = json.getJSONObject("result")
            val user = TelegramBotUser(
                id = result.getLong("id"),
                is_bot = result.optBoolean("is_bot", true),
                first_name = result.optString("first_name").takeIf { result.has("first_name") },
                username = result.optString("username").takeIf { result.has("username") },
                can_read_all_group_messages = if (result.has("can_read_all_group_messages")) result.getBoolean("can_read_all_group_messages") else null
            )
            ValidationResult.Success(user)
        } catch (e: Exception) {
            ValidationResult.Error("Validation failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Detailed bot-to-group diagnostic check.
     */
    suspend fun checkBotGroupMember(
        token: String,
        chatId: String,
        botUser: TelegramBotUser
    ): BotGroupMemberStatus = withContext(Dispatchers.IO) {
        val trimmedChat = chatId.trim()

        try {
            val chatUrl = "https://api.telegram.org/bot$token/getChat?chat_id=$trimmedChat"
            val (chatCode, chatBody) = executeGet(chatUrl)
            if (chatCode == null || chatBody == null) {
                return@withContext BotGroupMemberStatus.NetworkError("Cannot connect to Telegram servers. Please check your internet connection.")
            }
            val chatJson = JSONObject(chatBody)
            if (!chatJson.optBoolean("ok", false) || !chatJson.has("result")) {
                val desc = chatJson.optString("description", "")
                val params = chatJson.optJSONObject("parameters")
                val migrateId = params?.optLong("migrate_to_chat_id", 0L) ?: 0L
                if (migrateId != 0L) {
                    return@withContext BotGroupMemberStatus.Migrated(migrateId.toString(), "Supergroup")
                }
                return@withContext BotGroupMemberStatus.NotInGroup
            }

            val chatResult = chatJson.getJSONObject("result")
            val chatType = chatResult.optString("type", "")
            val groupTitle = chatResult.optString("title", "Private Group")

            if (chatType.equals("channel", ignoreCase = true)) {
                return@withContext BotGroupMemberStatus.NotGroup("channel")
            }
            if (chatType.equals("private", ignoreCase = true)) {
                return@withContext BotGroupMemberStatus.NotGroup("private")
            }

            if (botUser.can_read_all_group_messages == false) {
                return@withContext BotGroupMemberStatus.GroupPrivacyEnabled
            }

            val memberUrl = "https://api.telegram.org/bot$token/getChatMember?chat_id=$trimmedChat&user_id=${botUser.id}"
            val (mCode, memberBody) = executeGet(memberUrl)
            if (mCode == null || memberBody == null) {
                return@withContext BotGroupMemberStatus.NetworkError("Cannot connect to Telegram servers. Please check your internet connection.")
            }
            val memberJson = JSONObject(memberBody)
            if (!memberJson.optBoolean("ok", false) || !memberJson.has("result")) {
                return@withContext BotGroupMemberStatus.NotInGroup
            }

            val memberResult = memberJson.getJSONObject("result")
            val status = memberResult.optString("status", "").lowercase()
            if (status in listOf("left", "kicked")) {
                return@withContext BotGroupMemberStatus.NotInGroup
            }
            if (status == "restricted") {
                return@withContext BotGroupMemberStatus.Restricted
            }
            if (status !in listOf("administrator", "creator")) {
                return@withContext BotGroupMemberStatus.NotAdmin(status)
            }

            BotGroupMemberStatus.Success(groupTitle = groupTitle, groupType = chatType)
        } catch (e: Exception) {
            BotGroupMemberStatus.ApiError(e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Verifies group membership, administrator status, and disabled group privacy for a specific bot.
     */
    suspend fun verifyGroupBotMember(
        token: String,
        groupChatId: String,
        botUser: TelegramBotUser,
        roleName: String = "Bot"
    ): ValidationResult<String> = withContext(Dispatchers.IO) {
        val trimmedGroup = groupChatId.trim()
        val botHandle = botUser.username?.let { "@$it" } ?: "Bot"

        if (trimmedGroup.isBlank()) return@withContext ValidationResult.Error("Please enter the Group Chat ID.")
        if (!isValidGroupChatId(trimmedGroup)) return@withContext ValidationResult.Error("Invalid Group Chat ID format. Must start with '-' (e.g. -100123456789 or -123456789).")

        try {
            // 1. Verify Group Existence & Bot Access
            val chatUrl = "https://api.telegram.org/bot$token/getChat?chat_id=$trimmedGroup"
            val (chatCode, chatBody) = executeGet(chatUrl)
            if (chatCode == null || chatBody == null) {
                return@withContext ValidationResult.NetworkError("Cannot connect to Telegram servers. Please check your internet connection and try again.")
            }
            val chatJson = JSONObject(chatBody)
            if (!chatJson.optBoolean("ok", false) || !chatJson.has("result")) {
                return@withContext ValidationResult.Error(
                    "$roleName ($botHandle) is not in this group. Ensure 'Allow Groups?' is ON in @BotFather, and add $botHandle to the group."
                )
            }
            val groupTitle = chatJson.getJSONObject("result").optString("title", "Private Group")

            // 2. Verify Group Privacy is Disabled
            if (botUser.can_read_all_group_messages == false) {
                return@withContext ValidationResult.Error(
                    "Group Privacy is ENABLED for $roleName ($botHandle) in @BotFather.\nYou must disable Group Privacy for $botHandle in @BotFather (Bot Settings -> Group Privacy -> Turn OFF)."
                )
            }

            // 3. Verify Bot is Administrator in the Group
            val memberUrl = "https://api.telegram.org/bot$token/getChatMember?chat_id=$trimmedGroup&user_id=${botUser.id}"
            val (memberCode, memberBody) = executeGet(memberUrl)
            if (memberCode == null || memberBody == null) {
                return@withContext ValidationResult.NetworkError("Cannot connect to Telegram servers. Please check your internet connection and try again.")
            }
            val memberJson = JSONObject(memberBody)
            if (memberJson.optBoolean("ok", false) && memberJson.has("result")) {
                val memberResult = memberJson.getJSONObject("result")
                val status = memberResult.optString("status", "")
                if (status in listOf("left", "kicked")) {
                    return@withContext ValidationResult.Error("$roleName ($botHandle) has left or was removed from group '$groupTitle'. Please re-add $botHandle.")
                }
                if (status !in listOf("administrator", "creator")) {
                    return@withContext ValidationResult.Error(
                        "$roleName ($botHandle) is a regular member, NOT an Admin (Status: $status).\nPlease promote $botHandle to Administrator in Group Settings."
                    )
                }
            } else {
                return@withContext ValidationResult.Error("Could not verify permissions for $roleName ($botHandle) in group '$groupTitle'.")
            }

            ValidationResult.Success(groupTitle)
        } catch (e: Exception) {
            ValidationResult.Error("Validation failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Verifies the chat ID live with Telegram API for single bot (Telegram Mode or Client Mode).
     */
    suspend fun verifyChatId(
        token: String,
        chatId: String,
        botUser: TelegramBotUser,
        isPeerLinkEnabled: Boolean,
        requireGroupOnly: Boolean = false
    ): ValidationResult<String> = withContext(Dispatchers.IO) {
        val trimmed = chatId.trim()
        if (trimmed.isBlank()) return@withContext ValidationResult.Error("Please enter the Chat ID.")
        if (!isValidChatId(trimmed)) return@withContext ValidationResult.Error("Invalid Chat ID format.")

        val isGroup = trimmed.startsWith("-")

        if (requireGroupOnly && !isGroup) {
            return@withContext ValidationResult.Error("Admin mode requires a private Group ID (e.g. -100123456789). Positive IDs (private user chats) are not supported in Admin mode.")
        }

        try {
            val chatUrl = "https://api.telegram.org/bot$token/getChat?chat_id=$trimmed"
            val (code, chatBody) = executeGet(chatUrl)
            if (code == null || chatBody == null) {
                return@withContext ValidationResult.NetworkError("Cannot connect to Telegram servers. Please check your internet connection and try again.")
            }
            val chatJson = JSONObject(chatBody)
            if (!chatJson.optBoolean("ok", false) || !chatJson.has("result")) {
                return@withContext if (isGroup) {
                    ValidationResult.Error("Bot is not in this group. Enable 'Allow Groups' in @BotFather, and add @${botUser.username ?: "bot"} to the group.")
                } else {
                    ValidationResult.Error("Chat not found. Please ensure this user has started @${botUser.username ?: "bot"} on Telegram (send /start).")
                }
            }
            val chatResult = chatJson.getJSONObject("result")
            if (isGroup) {
                if (isPeerLinkEnabled) {
                    // Check Group Privacy
                    if (botUser.can_read_all_group_messages == false) {
                        return@withContext ValidationResult.Error("Group Privacy is Enabled in @BotFather. You must disable Group Privacy for this bot in @BotFather.")
                    }
                    // Check Admin Rights in Group
                    val memberUrl = "https://api.telegram.org/bot$token/getChatMember?chat_id=$trimmed&user_id=${botUser.id}"
                    val (mCode, memberBody) = executeGet(memberUrl)
                    if (mCode == null || memberBody == null) {
                        return@withContext ValidationResult.NetworkError("Cannot connect to Telegram servers. Please check your internet connection and try again.")
                    }
                    val memberJson = JSONObject(memberBody)
                    if (memberJson.optBoolean("ok", false) && memberJson.has("result")) {
                        val memberStatus = memberJson.getJSONObject("result").optString("status", "")
                        if (memberStatus in listOf("left", "kicked")) {
                            return@withContext ValidationResult.Error("Bot (@${botUser.username ?: "bot"}) is not in this group. Please invite @${botUser.username ?: "bot"} to the group.")
                        }
                        if (memberStatus !in listOf("administrator", "creator")) {
                            return@withContext ValidationResult.Error("Bot (@${botUser.username ?: "bot"}) is a regular member, NOT an Admin (Status: $memberStatus). Please promote @${botUser.username ?: "bot"} to Administrator in Group Settings.")
                        }
                    }
                }
                ValidationResult.Success(chatResult.optString("title", "Private Group"))
            } else {
                // Positive User ID
                val name = chatResult.optString("first_name").ifEmpty { chatResult.optString("username", "User") }
                ValidationResult.Success(name)
            }
        } catch (e: Exception) {
            ValidationResult.Error("Validation failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Validator for Partner Bot Username.
     */
    fun verifyPartnerUsername(
        partnerUsername: String,
        botUser: TelegramBotUser,
        isPeerLinkEnabled: Boolean,
        isGroup: Boolean,
        isAdminMode: Boolean = false
    ): ValidationResult<Unit> {
        if (!isPeerLinkEnabled || !isGroup) {
            return ValidationResult.Success(Unit)
        }

        val trimmed = partnerUsername.trim()
        if (trimmed.isBlank()) {
            if (isAdminMode) {
                return ValidationResult.Error("Partner Bot Username is required in Admin mode.")
            }
            return ValidationResult.Success(Unit)
        }

        if (!isValidPartnerBotUsername(trimmed, isAdminMode)) {
            return if (isAdminMode) {
                ValidationResult.Error("Must start with @ and end with 'bot' or '_bot' (e.g. @partner_bot).")
            } else {
                ValidationResult.Error("Must start with @ (e.g. @bot_username) and be between 4 and 32 characters.")
            }
        }

        val myBotUsername = botUser.username
        if (myBotUsername != null && trimmed.equals("@$myBotUsername", ignoreCase = true)) {
            return ValidationResult.Error("Partner Bot Username cannot be your own bot (@$myBotUsername). Enter your partner's bot username.")
        }

        return ValidationResult.Success(Unit)
    }
}

