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

@Database(entities = [MessageNode::class, ChatNode::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ThreadDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDb? = null

        fun getDatabase(context: Context): LocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDb::class.java,
                    "superior_chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
