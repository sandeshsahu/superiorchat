package com.mobile.superiorutils.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.dao.MessageDao

import com.mobile.superiorutils.data.entity.ChatNode
import com.mobile.superiorutils.data.dao.ThreadDao

import androidx.room.TypeConverters
import com.mobile.superiorutils.core.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MessageNode::class, ChatNode::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ThreadDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaFileName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE messages ADD COLUMN mediaFileSize INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): LocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDb::class.java,
                    "superior_chat_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
