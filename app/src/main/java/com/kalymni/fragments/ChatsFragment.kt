package com.kalymni.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kalymni.GetContacts
import com.kalymni.adapters.UserAdapter
import com.kalymni.databinding.FragmentChatsBinding
import com.kalymni.models.User
import java.lang.reflect.Type

class ChatsFragment : Fragment() {
    private lateinit var binding: FragmentChatsBinding
    private lateinit var list: ArrayList<User>
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: UserAdapter
    private lateinit var activity: AppCompatActivity
    private lateinit var context: Activity
    private lateinit var sharedModel: ArrayList<User>
    private lateinit var uIdList: ArrayList<String>
    private lateinit var nameList: ArrayList<String>
    private lateinit var imageList: ArrayList<String>
    private lateinit var tokenList: ArrayList<String>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPrefUserInfo: SharedPreferences
    private lateinit var editorUserInfo: SharedPreferences.Editor
    private lateinit var allUsers: ArrayList<User>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.activity = context as AppCompatActivity
        this.context = context
    }

    @SuppressLint("InlinedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        list = ArrayList()
        uIdList = ArrayList()
        nameList = ArrayList()
        imageList = ArrayList()
        tokenList = ArrayList()
        allUsers = ArrayList()
        sharedPref = context.applicationContext.getSharedPreferences("SUB_USERS", MODE_PRIVATE)
        editor = sharedPref.edit()
        sharedPrefUserInfo = context.getSharedPreferences("USER_INFO", MODE_PRIVATE)
        editorUserInfo = sharedPrefUserInfo.edit()

        binding.chatRecycle.layoutManager = LinearLayoutManager(context)
        adapter = UserAdapter(list, context)
        binding.chatRecycle.adapter = adapter

        saveUserInfo()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                200
            )
        }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) -> {
                loadData()
                fetchDataFromFirebase()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        binding.btnGoToAllUsers.setOnClickListener {
            startActivity(Intent(context, com.kalymni.AllUsers::class.java))
        }

        return binding.root
    }

    private fun loadData() {
        list.clear()
        val gson = Gson()
        val json = sharedPref.getString("subUsers", "")
        val type: Type = object : TypeToken<ArrayList<User>>() {
        }.type
        sharedModel = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
        list.addAll(sharedModel)
    }

    private fun saveUserInfo() {
        database.reference.child("Users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snp in snapshot.children) {
                        val user = snp.getValue(User::class.java)!!
                        if (user.id == FirebaseAuth.getInstance().uid) {
                            editorUserInfo.putString("name", user.userName)
                            editorUserInfo.putString("status", user.status)
                            editorUserInfo.putString("image", user.profilePic)
                            editorUserInfo.apply()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchDataFromFirebase() {
        database.reference.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                uIdList.clear()
                imageList.clear()
                nameList.clear()
                tokenList.clear()
                for (snp in snapshot.children) {
                    val user = snp.getValue(User::class.java)!!
                    for (contact in GetContacts(context).getContacts()) {
                        if (user.phone == contact.first && user.id != auth.uid) {
                            allUsers.add(user)
                        }
                    }
                }

                for (user in allUsers) {
                    uIdList.add(user.id.toString())
                    imageList.add(user.profilePic.toString())
                    tokenList.add(user.fcmToken.toString())
                    for (contact in GetContacts(context).getContacts()) {
                        if (contact.first == user.phone) {
                            nameList.add(contact.second)
                        }
                    }
                }

                database.reference.child("Chats")
                    .addValueEventListener(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(snapshot: DataSnapshot) {
                            sharedModel.clear()
                            for (snp2 in snapshot.children) {
                                for (user in allUsers) {
                                    if (auth.uid + user.id == snp2.key) {
                                        for (i in 0 until uIdList.size) {
                                            val usr = User()
                                            usr.id = uIdList[i]
                                            usr.profilePic = imageList[i]
                                            usr.fcmToken = tokenList[i]
                                            usr.userName = nameList[i]
                                            sharedModel.add(usr)
                                        }
                                    }
                                }
                            }
                            val gson = Gson()
                            val json = gson.toJson(sharedModel)
                            editor.remove("subUsers")
                            editor.apply()
                            editor.putString("subUsers", json)
                            editor.apply()
                            loadData()
                            adapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadData()
                fetchDataFromFirebase()
            } else {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    100
                )
            }
        }
}

