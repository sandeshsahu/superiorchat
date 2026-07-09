package com.mobile.superiorutils.core

import androidx.room.TypeConverter
import com.mobile.superiorutils.data.entity.MessageStatus

class Converters {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): Int {
        return status.code
    }

    @TypeConverter
    fun toMessageStatus(code: Int): MessageStatus {
        return MessageStatus.fromCode(code)
    }
}
