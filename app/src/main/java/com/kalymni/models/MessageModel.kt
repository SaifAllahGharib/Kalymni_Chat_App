package com.kalymni.models

data class MessageModel(
    var uId: String? = null,
    var message: String? = null,
    var timeStamp: Long? = null,
    var type: String? = null,
    var imageForSender: String? = null,
    var nameForSender: String? = null,
)