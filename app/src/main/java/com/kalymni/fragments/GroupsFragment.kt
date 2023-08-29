package com.kalymni.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kalymni.adapters.GroupChatAdapter
import com.kalymni.databinding.FragmentGroupsBinding
import com.kalymni.models.GroupModel
import java.lang.reflect.Type

class GroupsFragment : Fragment() {
    private lateinit var binding: FragmentGroupsBinding
    private val list: ArrayList<GroupModel> = ArrayList()
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: GroupChatAdapter
    private lateinit var activity: AppCompatActivity
    private lateinit var context: Activity
    private lateinit var sharedModel: ArrayList<GroupModel>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.activity = context as AppCompatActivity
        this.context = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        sharedPref = context.applicationContext.getSharedPreferences("GROUP_USERS", MODE_PRIVATE)
        editor = sharedPref.edit()
        binding.groupRecycle.layoutManager = LinearLayoutManager(context)
        adapter = GroupChatAdapter(context, list)
        binding.groupRecycle.adapter = adapter

        loadData()
        fetchDataFromFirebase()

        return binding.root
    }


    private fun loadData() {
        list.clear()
        val gson = Gson()
        val json = sharedPref.getString("groupUsers", "")
        val type: Type = object : TypeToken<ArrayList<GroupModel>>() {
        }.type
        sharedModel = if (!json.isNullOrEmpty()) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
        list.addAll(sharedModel)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchDataFromFirebase() {
        database.reference.child("Groups")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    list.clear()
                    sharedModel.clear()
                    for (snp in snapshot.children) {
                        val groupModel = snp.getValue(GroupModel::class.java)!!
                        sharedModel.add(groupModel)
                        val gson = Gson()
                        val json = gson.toJson(sharedModel)
                        editor.remove("groupUsers")
                        editor.apply()
                        editor.putString("groupUsers", json)
                        editor.apply()
                        loadData()
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}