package com.mobile.superiorutils.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.dao.MessageDao

import com.mobile.superiorutils.data.entity.ChatNode
import com.mobile.superiorutils.data.dao.ThreadDao

import com.mobile.superiorutils.data.entity.UserProfile
import com.mobile.superiorutils.data.dao.ProfileDao

import androidx.room.TypeConverters
import com.mobile.superiorutils.core.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobile.superiorutils.data.entity.EmojiUsage
import com.mobile.superiorutils.data.dao.EmojiDao

@Database(entities = [MessageNode::class, ChatNode::class, UserProfile::class, EmojiUsage::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ThreadDao
    abstract fun profileDao(): ProfileDao
    abstract fun emojiDao(): EmojiDao

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

        fun getDatabase(context: Context): LocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDb::class.java,
                    "superior_chat_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
