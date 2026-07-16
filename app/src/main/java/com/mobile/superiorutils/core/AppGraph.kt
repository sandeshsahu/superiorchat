package com.mobile.superiorutils.core

import android.content.Context
import androidx.room.Room
import com.mobile.superiorutils.data.Prefs
import com.mobile.superiorutils.data.repository.AppRepository

object AppGraph {
    
    private var isInitialized = false

    lateinit var prefs: Prefs
        private set

    lateinit var database: LocalDb
        private set

    lateinit var appRepository: AppRepository
        private set

    fun init(context: Context) {
        if (isInitialized) return
        
        val appContext = context.applicationContext
        
        prefs = Prefs.getInstance(appContext)
        
        database = LocalDb.getDatabase(appContext)

        appRepository = AppRepository(
            database.conversationDao(),
            database.messageDao(),
            database.profileDao(),
            database.emojiDao()
        )
        
        isInitialized = true
    }
}
