package com.kalymni

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.kalymni.adapters.AllUsersAdapter
import com.kalymni.databinding.ActivityAllUsersBinding
import com.kalymni.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AllUsers : AppCompatActivity() {
    private lateinit var binding: ActivityAllUsersBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var list: ArrayList<User>
    private lateinit var adapter: AllUsersAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedModel: ArrayList<User>
    private lateinit var uIdList: ArrayList<String>
    private lateinit var phoneList: ArrayList<String>
    private lateinit var nameList: ArrayList<String>
    private lateinit var statusList: ArrayList<String>
    private lateinit var imageList: ArrayList<String>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var contacts: GetContacts

    @SuppressLint("SetTextI18n", "Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        recyclerView = binding.recyclerViewAllUsers
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        list = ArrayList()
        uIdList = ArrayList()
        phoneList = ArrayList()
        nameList = ArrayList()
        statusList = ArrayList()
        imageList = ArrayList()
        sharedPref =
            applicationContext.getSharedPreferences("USERS", MODE_PRIVATE)
        editor = sharedPref.edit()
        contacts = GetContacts(this)

        binding.backAllUsers.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AllUsersAdapter(this, list)
        recyclerView.adapter = adapter

        if (ContextCompat.checkSelfPermission(
                this@AllUsers,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadData()
            fetchDataFromFirebase()
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this@AllUsers,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@AllUsers,
                arrayOf(
                    Manifest.permission.READ_CONTACTS
                ),
                100
            )
        } else {
            loadData()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadData() {
        list.clear()
        val gson = Gson()
        val json = sharedPref.getString("users", "")
        val type: Type = object : TypeToken<ArrayList<User>>() {
        }.type
        sharedModel = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
        binding.numOfContact.text = "${sharedModel.size} Contact"
        list.addAll(sharedModel)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadData()
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            checkPermissions()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchDataFromFirebase() {
        CoroutineScope(Dispatchers.Default).launch {
            val snapshot = suspendCoroutine { continuation ->
                database.reference.child("Users")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            continuation.resume(snapshot)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            list.clear()
            sharedModel.clear()
            for (snp in snapshot.children) {
                val model = snp.getValue(User::class.java)
                if (model!!.phone != auth.currentUser!!.phoneNumber) {
                    uIdList.add(model.id.toString())
                    phoneList.add(model.phone.toString())
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
                        if (nameList.size == uIdList.size) {
                            user.userName = nameList[i]
                        }
                        sharedModel.add(user)
                        break
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val gson = Gson()
                val json = gson.toJson(sharedModel)
//                editor.remove("users")
//                editor.apply()
                editor.putString("users", json)
                editor.apply()
                loadData()
                adapter.notifyDataSetChanged()
            }
        }
    }
}