package com.mobile.superiorchat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.superiorchat.data.entity.EmojiUsage

@Dao
interface EmojiDao {
    @Query("SELECT * FROM emoji_usage ORDER BY lastUsedAt DESC, usageCount DESC")
    fun getAllEmojis(): List<EmojiUsage>

    @Query("SELECT * FROM emoji_usage WHERE emoji = :emoji LIMIT 1")
    fun getEmoji(emoji: String): EmojiUsage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(emojiUsage: EmojiUsage)
}
