package com.mobile.superiorchat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mobile.superiorchat.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles WHERE chatId = :chatId")
    fun getProfile(chatId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE chatId = :chatId")
    suspend fun getProfileSync(chatId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile): Long
}
