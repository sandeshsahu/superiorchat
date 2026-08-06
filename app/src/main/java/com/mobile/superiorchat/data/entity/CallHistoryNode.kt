package com.mobile.superiorchat.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryNode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val durationSeconds: Long,
    val isMissed: Boolean = false,
    val callStatus: String = "COMPLETED",
    val peerJsId: String = "",
    val domain: String = "",
    val partnerName: String
)
