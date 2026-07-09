package com.mobile.superiorutils.core

import android.content.Context
import androidx.room.Room
import com.mobile.superiorutils.data.Prefs
import com.mobile.superiorutils.data.repository.DataSync

object AppGraph {
    
    private var isInitialized = false

    lateinit var prefs: Prefs
        private set

    lateinit var database: LocalDb
        private set

    lateinit var chatRepository: DataSync
        private set

    fun init(context: Context) {
        if (isInitialized) return
        
        val appContext = context.applicationContext
        
        prefs = Prefs.getInstance(appContext)
        
        database = Room.databaseBuilder(
            appContext,
            LocalDb::class.java,
            "superior_chat_database"
        )
        .fallbackToDestructiveMigration()
        .build()

        chatRepository = DataSync(
            database.conversationDao(),
            database.messageDao()
        )
        
        isInitialized = true
    }
}
