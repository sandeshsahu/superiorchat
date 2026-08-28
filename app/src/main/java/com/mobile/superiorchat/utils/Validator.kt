package com.mobile.superiorchat.utils

import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.bot.User

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

    fun isValidPartnerBotUsername(username: String, isAdminMode: Boolean = false): Boolean {
        if (username.isBlank()) return true
        return if (isAdminMode) {
            ADMIN_PARTNER_BOT_USERNAME_REGEX.matches(username.trim())
        } else {
            PARTNER_USERNAME_REGEX.matches(username.trim())
        }
    }

    /**
     * Live validator for Telegram Bot Token.
     * Hits Telegram getMe endpoint asynchronously to verify token validity and retrieve bot info.
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
     * Validator for Partner Bot Username.
     * Ensures username starts with @, matches Telegram username regex, and is not the bot's own username.
     */
    fun verifyPartnerUsername(
        partnerUsername: String,
        botUser: User,
        isPeerLinkEnabled: Boolean,
        isGroup: Boolean,
        isAdminMode: Boolean = false
    ): ValidationResult<Unit> {
        // If not PeerLink or not a group chat, partner username is optional / not applicable
        if (!isPeerLinkEnabled || !isGroup) {
            return ValidationResult.Success(Unit)
        }

        val trimmed = partnerUsername.trim()
        if (trimmed.isBlank()) {
            if (isAdminMode) {
                return ValidationResult.Error("Partner Bot Username is required in Admin mode.")
            }
            // Empty partner username is valid (open group mode)
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

    /**
     * Reusable validator for PeerLink bot-to-bot messages and system signals.
     * Ensures message belongs to the configured group and was sent by the authorized partner bot.
     */
    fun isAuthorizedPeerLinkMessage(
        msgChatId: String,
        fromUsername: String?,
        isBot: Boolean?,
        activeChatId: String,
        partnerBotUsername: String,
        isPeerLinkEnabled: Boolean
    ): Boolean {
        if (!isPeerLinkEnabled) return false
        if (!isValidGroupChatId(activeChatId)) return false
        if (msgChatId != activeChatId) return false
        if (isBot != true) return false
        val expectedPartner = partnerBotUsername.trim().removePrefix("@")
        val senderUser = fromUsername?.trim()?.removePrefix("@") ?: ""
        return expectedPartner.isNotBlank() && senderUser.equals(expectedPartner, ignoreCase = true)
    }

    /**
     * Reusable validator for internal system signal authorization.
     * Ensures control signals (e.g. [SYS-MSG-DELETE], [SYS-CALL-DECLINED]) originate from authorized senders.
     * In group chats (chatId < 0): Strictly requires a non-self bot (is_bot == true, id != myBotId),
     * matching partnerBotUsername if configured.
     * In direct DMs (chatId > 0): Strictly requires the sender ID to match activeChatId.
     */
    fun isAuthorizedSignalSender(
        msgChatId: String,
        fromUser: User?,
        activeChatId: String,
        partnerBotUsername: String,
        myBotId: String
    ): Boolean {
        if (msgChatId.isBlank() || activeChatId.isBlank() || fromUser == null) return false
        if (msgChatId != activeChatId) return false

        val isGroup = isValidGroupChatId(msgChatId)
        if (isGroup) {
            // Must be a bot, and cannot be our own bot reflected from Telegram
            if (fromUser.is_bot != true) return false
            if (myBotId.isNotBlank() && fromUser.id.toString() == myBotId) return false

            val expectedPartner = partnerBotUsername.trim().removePrefix("@")
            if (expectedPartner.isNotBlank()) {
                val senderUser = fromUser.username?.trim()?.removePrefix("@") ?: ""
                return senderUser.equals(expectedPartner, ignoreCase = true)
            }
            // If partner bot username was not configured yet, any non-self bot in the group is allowed
            return true
        } else {
            // Direct 1-on-1 DM: Partner is a human user chatting directly with our bot
            return fromUser.id.toString() == activeChatId
        }
    }

    sealed class SystemSignal {
        object None : SystemSignal()
        data class DeleteMessages(val messageIds: List<Long>) : SystemSignal()
        object CallDeclined : SystemSignal()
        data class ReactionUpdate(val messageId: Long, val emoji: String, val isClear: Boolean) : SystemSignal()
        data class BatchReactionUpdate(val updates: Map<Long, String>) : SystemSignal()
        data class Unknown(val rawSignal: String) : SystemSignal()
    }

    /**
     * Identifies and parses internal system signals from message text/caption.
     * Used at the core engine level to intercept, execute, and drop control signals before DB storage or UI notifications.
     */
    fun extractSystemSignal(rawText: String?): SystemSignal {
        if (rawText.isNullOrBlank()) return SystemSignal.None
        val trimmed = rawText.trim()

        // Case-insensitive regex: matches [SYS-MSG-DELETE:101], [sys_msg_delete: 101, 102], sys-msg-delete:101, etc.
        val deleteRegex = Regex("""(?:\[)?(?:SYS[-_]MSG[-_]DELETE)\s*:\s*([0-9,\s]+)(?:\])?""", RegexOption.IGNORE_CASE)
        val deleteMatch = deleteRegex.find(trimmed)
        if (deleteMatch != null) {
            val ids = deleteMatch.groupValues[1].split(",").mapNotNull { it.trim().toLongOrNull() }.take(100)
            if (ids.isNotEmpty()) return SystemSignal.DeleteMessages(ids)
        }

        // Case-insensitive regex: matches [SYS-CALL-DECLINED], sys_call_declined, etc.
        val declineRegex = Regex("""(?:\[)?(?:SYS[-_]CALL[-_]DECLINED)(?:\])?""", RegexOption.IGNORE_CASE)
        if (declineRegex.containsMatchIn(trimmed)) {
            return SystemSignal.CallDeclined
        }

        // Batch reaction regex: matches [SYS-REACTIONS: 101=❤️;102=🔥;103=CLEAR] or [SYS-REACTIONS: 101:❤️, 102:🔥]
        val batchReactionRegex = Regex("""(?:\[)?(?:SYS[-_]REACTIONS)\s*:\s*([^\]]+?)(?:\]|$)""", RegexOption.IGNORE_CASE)
        val batchMatch = batchReactionRegex.find(trimmed)
        if (batchMatch != null) {
            val payload = batchMatch.groupValues[1].trim()
            val updates = mutableMapOf<Long, String>()
            val entries = payload.split(Regex("""[;,]"""))
            for (entry in entries) {
                val pair = entry.trim().split(Regex("""[:=]"""), limit = 2)
                if (pair.size == 2) {
                    val id = pair[0].trim().toLongOrNull()
                    val rawEmoji = pair[1].trim()
                    if (id != null && rawEmoji.isNotBlank()) {
                        val isClear = rawEmoji.equals("CLEAR", ignoreCase = true) ||
                                      rawEmoji.equals("NONE", ignoreCase = true) ||
                                      rawEmoji.equals("NULL", ignoreCase = true)
                        updates[id] = if (isClear) "" else rawEmoji
                    }
                }
            }
            if (updates.isNotEmpty()) {
                return SystemSignal.BatchReactionUpdate(updates)
            }
        }

        // Single reaction regex: matches [SYS-REACTION: 1042, ❤️], [sys_reaction: 1042, CLEAR], SYS-REACTION:1042,👍
        val reactionRegex = Regex("""(?:\[)?(?:SYS[-_]REACTION)\s*:\s*(\d+)\s*[,=]?\s*(\S+?)(?:\]|$)""", RegexOption.IGNORE_CASE)
        val reactionMatch = reactionRegex.find(trimmed)
        if (reactionMatch != null) {
            val msgId = reactionMatch.groupValues[1].toLongOrNull()
            val emojiOrAction = reactionMatch.groupValues[2].trim()
            if (msgId != null && emojiOrAction.isNotBlank()) {
                val isClear = emojiOrAction.equals("CLEAR", ignoreCase = true) || 
                              emojiOrAction.equals("NONE", ignoreCase = true) ||
                              emojiOrAction.equals("NULL", ignoreCase = true)
                return SystemSignal.ReactionUpdate(
                    messageId = msgId,
                    emoji = if (isClear) "" else emojiOrAction,
                    isClear = isClear
                )
            }
        }

        // Catch-all for any other system signal prefix (case-insensitive)
        if (trimmed.startsWith("[SYS-", ignoreCase = true) || 
            trimmed.startsWith("[SYS_", ignoreCase = true) ||
            trimmed.startsWith("SYS-", ignoreCase = true) ||
            trimmed.startsWith("SYS_", ignoreCase = true)) {
            return SystemSignal.Unknown(trimmed)
        }

        return SystemSignal.None
    }

    /**
     * Pre-flight validator for Telegram Markdown syntax.
     * Uses strict delimiter parity (even/odd counts) and tag balancing to guarantee 100% validity.
     * Instantly catches unclosed single characters like "h_", "A_", "*test" so they are safely
     * transmitted as plain text on the first attempt without causing Telegram 400 entity errors.
     */
    fun hasValidMarkdownSyntax(text: String?): Boolean {
        if (text.isNullOrBlank()) return true

        // If no markdown formatting characters are present, it is safe plain text
        if (!text.contains('*') && !text.contains('_') && !text.contains('`') && 
            !text.contains('~') && !text.contains('|') && !text.contains('<')) {
            return true
        }

        // 1. Pre code blocks: ```...``` count must be even
        val tripleBacktickCount = text.windowed(3).count { it == "```" }
        if (tripleBacktickCount % 2 != 0) return false

        // Remove complete code blocks before inspecting inline delimiters
        val withoutCodeBlocks = text.replace(Regex("""```(?:[a-zA-Z0-9_-]+)?\n?[\s\S]*?```"""), "")

        // 2. Inline code backticks: count must be even
        if (withoutCodeBlocks.count { it == '`' } % 2 != 0) return false

        // 3. Spoilers: || must come in even pairs
        if (withoutCodeBlocks.windowed(2).count { it == "||" } % 2 != 0) return false

        // 4. Asterisks (*): every bold opening requires a closing pair (even count)
        if (withoutCodeBlocks.count { it == '*' } % 2 != 0) return false

        // 5. Underscores (_): in Telegram Markdown, odd underscores always break entities (e.g. h_, A_, _test)
        if (withoutCodeBlocks.count { it == '_' } % 2 != 0) return false

        // 6. Tildes (~): count must be even
        if (withoutCodeBlocks.count { it == '~' } % 2 != 0) return false

        // 7. Underline tags: <u> count must equal </u> count
        val uOpenCount = Regex("""<u>""", RegexOption.IGNORE_CASE).findAll(withoutCodeBlocks).count()
        val uCloseCount = Regex("""</u>""", RegexOption.IGNORE_CASE).findAll(withoutCodeBlocks).count()
        if (uOpenCount != uCloseCount) return false

        return true
    }

    /**
     * Strips Markdown formatting delimiters to provide clean text for clipboard copying, matching official Telegram behavior.
     */
    fun stripMarkdown(rawText: String?): String {
        if (rawText.isNullOrBlank()) return ""
        var clean = rawText
        // Code blocks: ```code``` -> code
        clean = clean.replace(Regex("""```(?:[a-zA-Z0-9_-]+)?\n?([\s\S]*?)```""")) { it.groupValues[1] }
        // Inline code: `code` -> code
        clean = clean.replace(Regex("""`([^`\n]+)`""")) { it.groupValues[1] }
        // Spoilers: ||text|| -> text
        clean = clean.replace(Regex("""\|\|([\s\S]+?)\|\|""")) { it.groupValues[1] }
        // Underline: <u>text</u> -> text
        clean = clean.replace(Regex("""<u>([\s\S]+?)</u>""", RegexOption.IGNORE_CASE)) { it.groupValues[1] }
        // Bold: **text** -> text, *text* -> text
        clean = clean.replace(Regex("""\*\*([^\*\n]+?)\*\*""")) { it.groupValues[1] }
        clean = clean.replace(Regex("""\*([^*\n]+?)\*""")) { it.groupValues[1] }
        // Italic: __text__ -> text, _text_ -> text
        clean = clean.replace(Regex("""__([^_\n]+?)__""")) { it.groupValues[1] }
        clean = clean.replace(Regex("""_([^_\n]+?)_""")) { it.groupValues[1] }
        // Strike: ~~text~~ -> text, ~text~ -> text
        clean = clean.replace(Regex("""~~([^~\n]+?)~~""")) { it.groupValues[1] }
        clean = clean.replace(Regex("""~([^~\n]+?)~""")) { it.groupValues[1] }
        return clean
    }

    /**
     * Reconstructs standard Telegram markdown formatting delimiters from Telegram MessageEntity objects.
     * When Telegram processes formatted messages sent by clients, it strips delimiters from `message.text`
     * and supplies character offset/length ranges in `entities`. This function wraps the designated substrings
     * in markdown formatting using reverse-offset insertion so earlier offsets remain unaffected.
     */
    fun applyTelegramEntities(rawText: String?, entities: List<com.mobile.superiorchat.bot.MessageEntity>?): String {
        if (rawText.isNullOrEmpty() || entities.isNullOrEmpty()) return rawText ?: ""

        // Sort descending by offset so modifications at the end of the text don't alter indices at the beginning
        val sortedEntities = entities.sortedByDescending { it.offset }
        var result: String = rawText

        for (entity in sortedEntities) {
            val start = entity.offset
            val end = minOf(entity.offset + entity.length, result.length)
            if (start in 0 until result.length && end in (start + 1)..result.length) {
                val target = result.substring(start, end)
                val wrapped = when (entity.type.lowercase()) {
                    "bold" -> "**$target**"
                    "italic" -> "_${target}_"
                    "code" -> "`$target`"
                    "pre" -> "```$target```"
                    "spoiler" -> "||$target||"
                    "underline" -> "<u>$target</u>"
                    "strikethrough" -> "~$target~"
                    else -> target
                }
                result = result.replaceRange(start, end, wrapped)
            }
        }
        return result
    }
}

