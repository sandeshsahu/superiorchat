package com.mobile.superiorutils.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val chatId: String,
    val title: String,
    val username: String,
    val type: String,
    val profilePhotoPath: String,
    val photoUniqueId: String
)
