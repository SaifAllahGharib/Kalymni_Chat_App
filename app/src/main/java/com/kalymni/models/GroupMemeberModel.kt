package com.kalymni.models

data class GroupMemberModel(
    var id: String? = null,
    var name: String? = null,
    var phone: String? = null,
    var role: String? = null,
    var status: String? = null,
    var image: String? = null,
    val token: String? = null
)