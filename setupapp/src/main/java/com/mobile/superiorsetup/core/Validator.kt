package com.mobile.superiorsetup.core

object Validator {
    private val BOT_TOKEN_REGEX = Regex("^\\d{8,10}:[A-Za-z0-9_-]{35}\$")
    private val CHAT_ID_REGEX = Regex("^-?\\d{7,15}\$")

    private val WEBRTC_URL_REGEX = Regex("^https?://([a-zA-Z0-9.-]+)(:\\d+)?/?\$")

    fun isValidBotToken(token: String): Boolean {
        return BOT_TOKEN_REGEX.matches(token)
    }

    fun isValidChatId(chatId: String): Boolean {
        return CHAT_ID_REGEX.matches(chatId)
    }

    fun isValidWebRtcUrl(url: String): Boolean {
        if (url.isEmpty()) return true
        return WEBRTC_URL_REGEX.matches(url.trim())
    }
}
