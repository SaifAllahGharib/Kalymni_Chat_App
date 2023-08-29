package com.kalymni.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    fun insertMessage(message: Message)

    @Query("delete from message_table where timeStamp=:timeStamp")
    fun deleteMessage(timeStamp: Long)

    @Query("select * from message_table order by timeStamp asc")
    fun getAllMessage(): LiveData<List<Message>>
}
