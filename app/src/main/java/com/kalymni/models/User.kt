package com.kalymni.models

class User(
    var id: String? = null,
    var userName: String? = null,
    var phone: String? = null,
    var status: String? = null,
    var profilePic: String? = null,
    var fcmToken: String? = null,
    var online: Boolean = true
)