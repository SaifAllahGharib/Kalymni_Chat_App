package com.kalymni

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.kalymni.adapters.EditeGroupAdapter
import com.kalymni.databinding.ActivityEditeGroupBinding
import com.kalymni.models.GroupModel
import com.kalymni.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditeGroup : AppCompatActivity(), EditeGroupAdapter.OnClickMembers {
    private lateinit var binding: ActivityEditeGroupBinding
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: EditeGroupAdapter
    private lateinit var list: ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditeGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        list = ArrayList()
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchMembersFormGroup()

        coroutineScope.launch(Dispatchers.IO) {
            loadImgForGroup(intent.getStringExtra("imageGroup").toString(), binding.editImageGroup)
        }

        binding.editImageGroup.setOnClickListener {
            uploadImage()
        }

        binding.nameGroup.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_change_name, null)
            builder.setView(view)

            val alert = builder.create()
            alert.show()

            view.findViewById<Button>(R.id.update_name).setOnClickListener {
                val name = view.findViewById<EditText>(R.id.et_update_name).text.toString()
                if (name.isNotEmpty()) {
                    database.reference.child("Groups")
                        .child(intent.getStringExtra("idGroup").toString()).child("name")
                        .setValue(name)
                    binding.nameGroup.text = name
                    alert.dismiss()
                } else {
                    Toast.makeText(this@EditeGroup, "Failed can not be empty", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        binding.nameGroup.text = intent.getStringExtra("nameGroup").toString()

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        adapter = EditeGroupAdapter(this, this, list)
        binding.recyclerViewMembersGroup.adapter = adapter
        binding.recyclerViewMembersGroup.layoutManager = LinearLayoutManager(this)
    }

    private suspend fun loadImgForGroup(url: String, imageView: ImageView) {
        try {
            val requestOptions = RequestOptions
                .placeholderOf(R.drawable.person)
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            val bitmap = Glide.with(this)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .submit()
                .get()

            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadImage() {
        val i = Intent()
        i.action = Intent.ACTION_GET_CONTENT
        i.type = "image/*"
        startActivityForResult(i, 100)
    }

    private fun fetchMembersFormGroup() {
        database.reference.child("Groups").child(intent.getStringExtra("idGroup").toString())
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        list.clear()
                        val group = snapshot.getValue(GroupModel::class.java)!!

                        for ((k, v) in group.Members!!.entries) {
                            list.add(User(v.id, v.name, null, v.status, v.image))
                        }
                        adapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        println(e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            if (data.data != null) {
                val imageUri = data.data!!
                binding.editImageGroup.setImageURI(imageUri)
                val ref = FirebaseStorage.getInstance().reference.child("Groups images")
                    .child(intent.getStringExtra("idGroup").toString())
                ref.putFile(imageUri).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        ref.downloadUrl.addOnSuccessListener { pic ->
                            database.reference.child("Groups")
                                .child(intent.getStringExtra("idGroup").toString()).child("image")
                                .setValue(pic.toString())
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onClickMember(position: Int) {
        if (list[position].id != auth.uid) {
            val builder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_member_group, null)
            builder.setView(view)

            val alert = builder.create()
            alert.show()

            view.findViewById<TextView>(R.id.addAdmin).setOnClickListener {
                database.reference.child("Groups")
                    .child(intent.getStringExtra("idGroup").toString())
                    .child("Members").child(list[position].id.toString()).child("role")
                    .setValue("admin")
                alert.dismiss()
            }

            view.findViewById<TextView>(R.id.leaveFromGroup).setOnClickListener {
                database.reference.child("Groups")
                    .child(intent.getStringExtra("idGroup").toString())
                    .child("Members").child(list[position].id.toString()).removeValue()
                alert.dismiss()
            }
        }
    }
}