package com.kalymni

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.kalymni.databinding.ActivityStatusBinding
import com.kalymni.models.User

class Status : AppCompatActivity() {
    private lateinit var binding: ActivityStatusBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var nameInput: TextInputLayout
    private lateinit var statusInput: TextInputLayout
    private lateinit var storage: FirebaseStorage
    private lateinit var model: User
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        nameInput = binding.name
        statusInput = binding.status
        model = User()
        sharedPref =
            applicationContext.getSharedPreferences("signing", MODE_PRIVATE)
        editor = sharedPref.edit()

        binding.revAddPic.setOnClickListener {
            uploadImage()
        }

        binding.btnFinish.setOnClickListener {
            if (nameInput.editText!!.text.isEmpty() || statusInput.editText!!.text.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                if (nameInput.editText!!.text.isEmpty()) {
                    builder.setMessage("Please enter your name.")
                } else if (statusInput.editText!!.text.isEmpty()) {
                    builder.setMessage("Please enter your status.")
                }
                builder.setCancelable(false)
                builder.setPositiveButton("ok") { d, _ ->
                    d.cancel()
                }
                val alertDialog = builder.create()
                alertDialog.show()
            } else if (nameInput.editText!!.text.isNotEmpty() && statusInput.editText!!.text.isNotEmpty()) {
                editor.putBoolean("signed", true)
                editor.apply()
                uploadData()
            }
        }
    }

    private fun uploadData() {
        binding.btnFinish.visibility = View.GONE
        binding.prog.visibility = View.VISIBLE
        model.id = FirebaseAuth.getInstance().uid
        model.userName = nameInput.editText!!.text.toString()
        model.phone = intent.getStringExtra("phone").toString()
        model.status = statusInput.editText!!.text.toString()
        database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .setValue(model).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.btnFinish.visibility = View.VISIBLE
                    binding.prog.visibility = View.GONE
                    startActivity(Intent(this@Status, MainActivity::class.java))
                    finish()
                }
            }
    }

    private fun uploadImage() {
        val i = Intent()
        i.action = Intent.ACTION_GET_CONTENT
        i.type = "image/*"
        startActivityForResult(i, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null) {
            val selectedImg = data.data!!
            binding.addPictcher.setImageURI(selectedImg)
            val ref = storage.reference.child("profile_picture")
                .child(FirebaseAuth.getInstance().uid!!)
            ref.putFile(selectedImg).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { pic ->
                    model.profilePic = pic.toString()
                    database.reference.child("Users")
                        .child(FirebaseAuth.getInstance().uid!!)
                        .setValue(model)
                }
            }
        }
    }
}