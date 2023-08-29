package com.kalymni.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_table")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val uId: String,
    val message: String,
    val timeStamp: Long,
    val type: String
)