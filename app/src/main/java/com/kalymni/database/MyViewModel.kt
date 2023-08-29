package com.kalymni.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: Repository

    init {
        repository = Repository(application)
    }

    fun insertMessage(message: Message) {
        repository.insertMessage(message)
    }

    fun deleteMessage(timeStamp: Long) {
        repository.deleteMessage(timeStamp)
    }

    fun getAllMessage(): LiveData<List<Message>> {
        return repository.getAllMessage()
    }
}