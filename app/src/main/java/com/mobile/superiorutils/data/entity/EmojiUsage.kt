package com.mobile.superiorutils.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emoji_usage")
data class EmojiUsage(
    @PrimaryKey val emoji: String,
    val usageCount: Int,
    val lastUsedAt: Long
)