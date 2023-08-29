package com.kalymni

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.kalymni.databinding.ActivityCreateGroupBinding
import com.kalymni.models.GroupMemberModel
import com.kalymni.models.GroupModel

class CreateGroup : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var imageUri: Uri
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var usersIdList: ArrayList<String>
    private lateinit var usersNameList: ArrayList<String>
    private lateinit var usersPhoneList: ArrayList<String>
    private lateinit var statusList: ArrayList<String>
    private lateinit var imageList: ArrayList<String>
    private lateinit var storage: FirebaseStorage
    private lateinit var groupId: String
    private lateinit var groupModel: GroupModel

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        usersIdList = ArrayList()
        usersNameList = ArrayList()
        usersPhoneList = ArrayList()
        statusList = ArrayList()
        imageList = ArrayList()
        storage = FirebaseStorage.getInstance()
        groupModel = GroupModel()
        groupId = database.getReference("Groups").push().key.toString()

        var index = 0
        while (intent.hasExtra("id${index + 1}")) {
            usersIdList.add(intent.getStringExtra("id${index + 1}").toString())
            usersNameList.add(intent.getStringExtra("name${index + 1}").toString())
            usersPhoneList.add(intent.getStringExtra("phone${index + 1}").toString())
            statusList.add(intent.getStringExtra("status${index + 1}").toString())
            imageList.add(intent.getStringExtra("image${index + 1}").toString())
            index++
        }

        binding.backCreateGroup.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.nameGroup.editText!!.text.trimEnd().trimStart().toString()
            if (groupName.isEmpty()) {
                Toast.makeText(
                    this@CreateGroup,
                    "Name group failed required",
                    Toast.LENGTH_SHORT
                ).show()
                binding.nameGroup.editText!!.requestFocus()
            } else {
                val databaseRef = database.getReference("Groups")
                groupModel.id = groupId
                groupModel.adminId = auth.uid
                groupModel.createdAt = System.currentTimeMillis().toString()
                groupModel.adminName = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                    "name",
                    ""
                )
                groupModel.name = binding.nameGroup.editText!!.text.toString()
                databaseRef.child(groupId).setValue(groupModel)

                val dataRef = FirebaseDatabase.getInstance().getReference("Groups").child(groupId)
                    .child("Members")
                val modelAdmin = GroupMemberModel()
                modelAdmin.id = auth.uid
                modelAdmin.name = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                    "name",
                    ""
                )
                modelAdmin.role = "admin"
                modelAdmin.status = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                    "status",
                    ""
                )
                modelAdmin.image = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                    "image",
                    ""
                )
                dataRef.child(auth.uid.toString())
                    .setValue(modelAdmin)
                for (i in 0 until usersIdList.size) {
                    val modelMember = GroupMemberModel()
                    modelMember.id = usersIdList[i]
                    modelMember.name = usersNameList[i]
                    modelMember.phone = usersPhoneList[i]
                    modelMember.status = statusList[i]
                    modelMember.image = imageList[i]
                    modelMember.role = "member"
                    dataRef.child(usersIdList[i])
                        .setValue(modelMember).addOnSuccessListener {
                            finish()
                        }
                }
            }
        }

        binding.addPictcherGroup.setOnClickListener {
            val i = Intent()
            i.action = Intent.ACTION_GET_CONTENT
            i.type = "image/*"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        i.addCategory("android.intent.category.DEFAULT")
                        i.data = Uri.parse(
                            String.format(
                                "package:%s",
                                applicationContext.packageName
                            )
                        )
                        startActivityIfNeeded(i, 101)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println(e)
                    }
                } else {
                    startActivityForResult(i, 300)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            if (data.data != null) {
                imageUri = data.data!!
                binding.addPictcherGroup.setImageURI(imageUri)
                val ref = storage.reference.child("Groups images")
                    .child(groupId)
                ref.putFile(imageUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        ref.downloadUrl.addOnSuccessListener { pic ->
                            groupModel.image = pic.toString()
                            database.reference.child("Groups")
                                .child(groupId).child("image")
                                .setValue(pic.toString())
                        }
                    }
                }
            }
        }
    }
}