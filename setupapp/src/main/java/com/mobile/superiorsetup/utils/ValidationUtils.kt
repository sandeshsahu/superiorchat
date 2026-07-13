package com.mobile.superiorsetup.utils

object ValidationUtils {
    private val BOT_TOKEN_REGEX = Regex("^\\d{8,10}:[A-Za-z0-9_-]{35}\$")
    private val CHAT_ID_REGEX = Regex("^-?\\d{7,15}\$")

    fun isValidBotToken(token: String): Boolean {
        return BOT_TOKEN_REGEX.matches(token)
    }

    fun isValidChatId(chatId: String): Boolean {
        return CHAT_ID_REGEX.matches(chatId)
    }
}
