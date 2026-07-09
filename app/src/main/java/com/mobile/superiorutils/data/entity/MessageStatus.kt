package com.mobile.superiorutils.data.entity

enum class MessageStatus(val code: Int) {
    SENDING(0),
    SENT(1),
    FAILED(2),
    READ(3),
    QUEUED(4);

    companion object {
        fun fromCode(code: Int): MessageStatus {
            return entries.find { it.code == code } ?: SENDING
        }
    }
}
