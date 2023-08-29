package com.kalymni.database

import android.app.Application
import androidx.lifecycle.LiveData

class Repository(application: Application) {
    private val messageDao: MessageDao

    init {
        val db = MyRoomDatabase.getDatabase(application)
        messageDao = db.messageDao()
    }

    fun insertMessage(message: Message) {
        MyRoomDatabase.databaseWriteExecutor.execute {
            messageDao.insertMessage(message)
        }
    }

    fun deleteMessage(timeStamp: Long) {
        MyRoomDatabase.databaseWriteExecutor.execute {
            messageDao.deleteMessage(timeStamp)
        }
    }

    fun getAllMessage(): LiveData<List<Message>> {
        return messageDao.getAllMessage()
    }
}