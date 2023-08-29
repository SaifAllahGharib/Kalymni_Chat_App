package com.kalymni.models

data class GroupModel(
    var id: String? = null,
    var Members: HashMap<String, GroupMemberModel>? = null,
    var adminId: String? = null,
    var adminName: String? = null,
    var createdAt: String? = null,
    var image: String? = null,
    var name: String? = null
)