package com.mobile.superiorchat.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.dao.MessageDao

import com.mobile.superiorchat.data.entity.ChatNode
import com.mobile.superiorchat.data.dao.ThreadDao

import com.mobile.superiorchat.data.entity.UserProfile
import com.mobile.superiorchat.data.dao.ProfileDao

import androidx.room.TypeConverters
import com.mobile.superiorchat.core.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobile.superiorchat.data.entity.EmojiUsage
import com.mobile.superiorchat.data.dao.EmojiDao
import com.mobile.superiorchat.data.entity.CallHistoryNode
import com.mobile.superiorchat.data.dao.CallHistoryDao

@Database(entities = [MessageNode::class, ChatNode::class, UserProfile::class, EmojiUsage::class, CallHistoryNode::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ThreadDao
    abstract fun profileDao(): ProfileDao
    abstract fun emojiDao(): EmojiDao
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaFileName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaFileSize INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages_new (
                        messageId INTEGER NOT NULL,
                        conversationId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        text TEXT,
                        timestamp INTEGER NOT NULL,
                        isFromMe INTEGER NOT NULL,
                        mediaType TEXT,
                        mediaUrl TEXT,
                        mediaLocalPath TEXT,
                        status INTEGER NOT NULL,
                        mediaFileName TEXT,
                        mediaFileSize INTEGER,
                        PRIMARY KEY(messageId),
                        FOREIGN KEY(conversationId) REFERENCES conversations(chatId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO messages_new SELECT * FROM messages")
                db.execSQL("DROP TABLE messages")
                db.execSQL("ALTER TABLE messages_new RENAME TO messages")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_status` ON `messages` (`status`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN bio TEXT")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN inviteLink TEXT")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN hasProtectedContent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN isForum INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToMessageId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN reactions TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `emoji_usage` (`emoji` TEXT NOT NULL, `usageCount` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`emoji`))")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `call_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `isMissed` INTEGER NOT NULL, `partnerName` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_history ADD COLUMN callStatus TEXT NOT NULL DEFAULT 'COMPLETED'")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_history ADD COLUMN peerJsId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE call_history ADD COLUMN domain TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_history ADD COLUMN isIncoming INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. messages
                db.execSQL("ALTER TABLE messages ADD COLUMN editTimestamp INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN isFromBot INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderUsername TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderPhotoPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN senderRole TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN chatType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaMimeType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaDuration INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaWidth INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaHeight INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaThumbPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaWaveform TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN entities TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToText TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN replyToAuthor TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN forwardFromId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN forwardFromName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN forwardDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN expiresAt INTEGER DEFAULT NULL")

                // 2. conversations
                db.execSQL("ALTER TABLE conversations ADD COLUMN pinnedMessageIds TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN chatType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN description TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN photoPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN memberCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN draftText TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN draftTimestamp INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE conversations ADD COLUMN ttlSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN wallpaperPath TEXT DEFAULT NULL")

                // 3. user_profiles
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN isBot INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN isVerified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN userRole TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN phoneNumber TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN customNickname TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN languageCode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN lastSeenTimestamp INTEGER DEFAULT NULL")

                // 4. call_history
                db.execSQL("ALTER TABLE call_history ADD COLUMN callType TEXT NOT NULL DEFAULT 'voice'")
                db.execSQL("ALTER TABLE call_history ADD COLUMN partnerPhotoPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE call_history ADD COLUMN endReason TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE call_history ADD COLUMN bytesTransferred INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): LocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDb::class.java,
                    "superior_chat_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
