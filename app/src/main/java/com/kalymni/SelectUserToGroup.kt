@file:OptIn(DelicateCoroutinesApi::class)

package com.kalymni

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kalymni.adapters.UserGroupAdapter
import com.kalymni.databinding.ActivitySelectUserToGroupBinding
import com.kalymni.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class SelectUserToGroup : AppCompatActivity() {
    private lateinit var binding: ActivitySelectUserToGroupBinding
    private lateinit var userRv: RecyclerView
    private lateinit var userList: ArrayList<User>
    private lateinit var userAdapter: UserGroupAdapter
    private lateinit var userSelectRv: RecyclerView
    private lateinit var userSelectList: ArrayList<User>
    private lateinit var userSelectedAdapter: UserGroupAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedModel: ArrayList<User>
    private lateinit var uIdList: ArrayList<String>
    private lateinit var nameSNP: ArrayList<String>
    private lateinit var nameList: ArrayList<String>
    private lateinit var phoneList: ArrayList<String>
    private lateinit var statusList: ArrayList<String>
    private lateinit var imageList: ArrayList<String>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var contacts: GetContacts
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectUserToGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        userRv = binding.recyclerGroup
        userSelectRv = binding.recyclerUserSelected
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        userList = ArrayList()
        userSelectList = ArrayList()
        uIdList = ArrayList()
        nameSNP = ArrayList()
        nameList = ArrayList()
        phoneList = ArrayList()
        statusList = ArrayList()
        imageList = ArrayList()
        sharedPref =
            applicationContext.getSharedPreferences("USERS_GROUP", MODE_PRIVATE)
        editor = sharedPref.edit()
        contacts = GetContacts(this)

        // Recycler View 1
        userRv.layoutManager = LinearLayoutManager(this)
        userAdapter =
            UserGroupAdapter(
                this,
                userList,
                R.layout.sample_show_users,
                false
            ) { position ->
                val selectedUser = userList[position]
                val index = userSelectList.indexOf(selectedUser)
                if (index == -1) {
                    userSelectList.add(selectedUser)
                    if (userSelectList.isNotEmpty()) {
                        binding.allUsersToGroup.visibility = View.VISIBLE
                        binding.hr.visibility = View.VISIBLE
                    }
                } else {
                    userSelectList.removeAt(index)
                    if (userSelectList.isEmpty()) {
                        binding.allUsersToGroup.visibility = View.GONE
                        binding.hr.visibility = View.GONE
                    }
                }
                userSelectedAdapter.notifyDataSetChanged()
            }
        userRv.adapter = userAdapter

        // Recycler View 2
        userSelectRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        userSelectedAdapter =
            UserGroupAdapter(this, userSelectList, R.layout.sample_selected_users, true) {}
        userSelectRv.adapter = userSelectedAdapter

        binding.backNewGroup.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (ContextCompat.checkSelfPermission(
                this@SelectUserToGroup,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchDataFromFirebase()
        }

        loadData()

        var index = 0
        binding.btnGoToNameGroup.setOnClickListener {
            val i = Intent(this@SelectUserToGroup, CreateGroup::class.java)
            for (inx in 0 until userSelectList.size) {
                index++
                i.putExtra("id$index", userSelectList[inx].id.toString())
                i.putExtra("name$index", nameSNP[inx])
                i.putExtra("phone$index", phoneList[inx])
                i.putExtra("status$index", statusList[inx])
                i.putExtra("image$index", imageList[inx])
            }

            if (userSelectList.isEmpty()) {
                Toast.makeText(this, "At 1 last contact must be selected", Toast.LENGTH_SHORT)
                    .show()
            } else {
                startActivity(i)
                finish()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadData() {
        userList.clear()
        val gson = Gson()
        val json = sharedPref.getString("usersGroup", "")
        val type: Type = object : TypeToken<ArrayList<User>>() {
        }.type
        sharedModel = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
        userList.addAll(sharedModel)
    }

    private fun fetchDataFromFirebase() {
        database.reference.child("Users")
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    sharedModel.clear()
                    for (snp in snapshot.children) {
                        val model = snp.getValue(User::class.java)
                        if (model!!.phone != auth.currentUser!!.phoneNumber) {
                            uIdList.add(model.id.toString())
                            phoneList.add(model.phone.toString())
                            nameSNP.add(model.userName.toString())
                            statusList.add(model.status.toString())
                            imageList.add(model.profilePic.toString())

                            for (contact in contacts.getContacts()) {
                                if (contact.first == model.phone) {
                                    nameList.add(contact.second)
                                }
                            }
                        }
                    }

                    for (i in 0 until uIdList.size) {
                        for (contact in contacts.getContacts()) {
                            if (phoneList.contains(contact.first)) {
                                val user = User()
                                user.id = uIdList[i]
                                user.phone = phoneList[i]
                                user.status = statusList[i]
                                user.profilePic = imageList[i]
                                user.userName = nameList[i]
                                sharedModel.add(user)
                                break
                            }
                        }
                    }

                    val gson = Gson()
                    val json = gson.toJson(sharedModel)
                    editor.remove("usersGroup")
                    editor.apply()
                    editor.putString("usersGroup", json)
                    editor.apply()
                    loadData()
                    userAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    FirebaseDatabase.getInstance().reference.child("Users")
                        .child(FirebaseAuth.getInstance().uid!!)
                        .child("online")
                        .setValue(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1000)
    }

    override fun onStop() {
        super.onStop()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                FirebaseDatabase.getInstance().reference.child("Users")
                    .child(FirebaseAuth.getInstance().uid!!)
                    .child("online")
                    .setValue(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        coroutineScope.cancel()
    }
}
