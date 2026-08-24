package com.mobile.superiorchat.utils

import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.bot.User

object Validator {
    private val BOT_TOKEN_REGEX = Regex("^\\d{8,10}:[A-Za-z0-9_-]{35}\$")
    private val CHAT_ID_REGEX = Regex("^-?\\d{7,15}\$")
    private val GROUP_CHAT_ID_REGEX = Regex("^-(?:100\\d{9,13}|\\d{6,12})\$")
    private val WEBRTC_URL_REGEX = Regex("^https?://([a-zA-Z0-9.-]+)(:\\d+)?/?\$")
    private val PARTNER_BOT_USERNAME_REGEX = Regex("^@[a-zA-Z0-9_]{3,32}\$")

    sealed class ValidationResult<out T> {
        data class Success<T>(val data: T) : ValidationResult<T>()
        data class Error(val message: String) : ValidationResult<Nothing>()
    }

    fun isValidBotToken(token: String): Boolean {
        return BOT_TOKEN_REGEX.matches(token)
    }

    fun isValidChatId(chatId: String): Boolean {
        return CHAT_ID_REGEX.matches(chatId)
    }

    fun isValidGroupChatId(chatId: String): Boolean {
        return GROUP_CHAT_ID_REGEX.matches(chatId.trim())
    }

    fun isValidWebRtcUrl(url: String): Boolean {
        if (url.isEmpty()) return true
        return WEBRTC_URL_REGEX.matches(url.trim())
    }

    fun isValidPartnerBotUsername(username: String): Boolean {
        if (username.isBlank()) return true
        return PARTNER_BOT_USERNAME_REGEX.matches(username.trim())
    }

    /**
     * Verifies the bot token live with Telegram API.
     */
    suspend fun verifyBotToken(token: String): ValidationResult<User> {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return ValidationResult.Error("Please enter your Telegram Bot Token.")
        if (!isValidBotToken(trimmed)) return ValidationResult.Error("Invalid Bot Token format. It should look like 1234567890:AAH...")

        return try {
            val resp = TelegramApi.getMeSuspend(trimmed)
            if (resp == null || !resp.ok || resp.result == null) {
                ValidationResult.Error("Invalid bot token or token has been revoked by @BotFather.")
            } else {
                ValidationResult.Success(resp.result)
            }
        } catch (e: java.io.IOException) {
            ValidationResult.Error("Cannot connect to Telegram servers. Please check your internet connection.")
        } catch (e: Exception) {
            ValidationResult.Error("Validation failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Verifies the chat ID live with Telegram API.
     * @param token Verified bot token
     * @param chatId Entered chat ID (can be negative group or positive user ID)
     * @param botUser Verified bot user from token check
     * @param isPeerLinkEnabled Whether PeerLink is active
     * @param requireGroupOnly If true (Admin mode), rejects positive user IDs; if false (Client mode), allows positive user IDs.
     */
    suspend fun verifyChatId(
        token: String,
        chatId: String,
        botUser: User,
        isPeerLinkEnabled: Boolean,
        requireGroupOnly: Boolean = false
    ): ValidationResult<String> {
        val trimmed = chatId.trim()
        if (trimmed.isBlank()) return ValidationResult.Error("Please enter the Chat ID.")
        if (!isValidChatId(trimmed)) return ValidationResult.Error("Invalid Chat ID format.")

        val isGroup = trimmed.startsWith("-")

        if (requireGroupOnly && !isGroup) {
            return ValidationResult.Error("Admin mode requires a private Group ID (e.g. -100123456789). Positive IDs (private user chats) are not supported in Admin mode.")
        }

        return try {
            val chatResp = TelegramApi.getChatSuspend(token, trimmed)
            if (chatResp == null || !chatResp.ok || chatResp.result == null) {
                if (isGroup) {
                    ValidationResult.Error("Bot is not in this group. Enable 'Allow Groups' in @BotFather, and add @${botUser.username ?: "bot"} to the group.")
                } else {
                    ValidationResult.Error("Chat not found. Please ensure this user has started @${botUser.username ?: "bot"} on Telegram (send /start).")
                }
            } else {
                val chat = chatResp.result
                if (isGroup) {
                    if (isPeerLinkEnabled) {
                        // Check Group Privacy
                        if (botUser.can_read_all_group_messages == false) {
                            return ValidationResult.Error("Group Privacy is Enabled in @BotFather. You must disable Group Privacy and enable Bot-to-Bot Communication Mode for this bot in @BotFather.")
                        }
                        // Check Admin Rights in Group
                        val member = TelegramApi.getChatMember(token, trimmed, botUser.id)
                        if (member == null || member.status in listOf("left", "kicked")) {
                            return ValidationResult.Error("Bot (@${botUser.username ?: "bot"}) is not in this group. Please invite @${botUser.username ?: "bot"} to the group.")
                        }
                        if (member.status !in listOf("administrator", "creator")) {
                            return ValidationResult.Error("Bot (@${botUser.username ?: "bot"}) is a regular member, NOT an Admin (Status: ${member.status}). Please promote @${botUser.username ?: "bot"} to Administrator in Group Settings.")
                        }
                    }
                    ValidationResult.Success(chat.title ?: "Private Group")
                } else {
                    // Positive User ID
                    val name = chat.first_name ?: chat.username ?: "User"
                    ValidationResult.Success("Connected: $name (Direct DM)")
                }
            }
        } catch (e: java.io.IOException) {
            ValidationResult.Error("Cannot connect to Telegram servers. Please check your internet connection.")
        } catch (e: Exception) {
            ValidationResult.Error("Validation failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Verifies partner bot username.
     */
    fun verifyPartnerUsername(
        partnerUsername: String,
        botUser: User,
        isPeerLinkEnabled: Boolean,
        isGroup: Boolean
    ): ValidationResult<Unit> {
        // If not PeerLink or not a group chat, partner username is optional / not applicable
        if (!isPeerLinkEnabled || !isGroup) {
            return ValidationResult.Success(Unit)
        }

        val trimmed = partnerUsername.trim()
        if (trimmed.isBlank()) {
            // Empty partner username is valid (open group mode)
            return ValidationResult.Success(Unit)
        }

        if (!isValidPartnerBotUsername(trimmed)) {
            return ValidationResult.Error("Must start with @ (e.g. @partner_bot) and be between 4 and 32 characters.")
        }

        val myBotUsername = botUser.username
        if (myBotUsername != null && trimmed.equals("@$myBotUsername", ignoreCase = true)) {
            return ValidationResult.Error("Partner Bot Username cannot be your own bot (@$myBotUsername). Enter your partner's bot username.")
        }

        return ValidationResult.Success(Unit)
    }
}
