package com.kalymni

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.kalymni.databinding.ActivityEditProfileBinding

class EditProfile : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase
    private lateinit var dialog: AlertDialog
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        sharedPref = getSharedPreferences("USER_INFO", MODE_PRIVATE)

        Glide.with(this)
            .load(sharedPref.getString("image", ""))
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.editImageProfile)



        binding.name.text = sharedPref.getString("name", "")
        binding.status.text = sharedPref.getString("status", "")

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }

        binding.editImageProfile.setOnClickListener {
            if (NetworkStatus(this).isNetworkConnected()) {
                uploadImage()
            } else {
                createSnackBar()
            }
        }

        binding.changeName.setOnClickListener {
            if (NetworkStatus(this).isNetworkConnected()) {
                changeData("name")
            } else {
                createSnackBar()
            }
        }

        binding.changeStatus.setOnClickListener {
            if (NetworkStatus(this).isNetworkConnected()) {
                changeData("status")
            } else {
                createSnackBar()
            }
        }
    }

    private fun uploadImage() {
        val i = Intent()
        i.action = Intent.ACTION_GET_CONTENT
        i.type = "image/*"
        startActivityForResult(i, 100)
    }

    private fun changeData(type: String) {
        if (type == "name") {
            val view = createAlertDialog(type)
            view.findViewById<Button>(R.id.update_name).setOnClickListener {
                if (view.findViewById<EditText>(R.id.et_update_name).text.toString().isNotEmpty()) {
                    database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                        .child("userName")
                        .setValue(view.findViewById<EditText>(R.id.et_update_name).text.toString())
                    binding.name.text =
                        view.findViewById<EditText>(R.id.et_update_name).text.toString()
                    view.findViewById<EditText>(R.id.et_update_name).setText("")
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "The Field can not be empty", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val view = createAlertDialog(type)
            view.findViewById<Button>(R.id.update_status).setOnClickListener {
                if (view.findViewById<EditText>(R.id.et_update_status).text.toString()
                        .isNotEmpty()
                ) {
                    database.reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
                        .child("status")
                        .setValue(view.findViewById<EditText>(R.id.et_update_status).text.toString())
                    binding.status.text =
                        view.findViewById<EditText>(R.id.et_update_status).text.toString()
                    view.findViewById<EditText>(R.id.et_update_status).setText("")
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "The Field can not be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createAlertDialog(type: String): View {
        val builder = AlertDialog.Builder(this)
        val view = if (type == "name") {
            LayoutInflater.from(this)
                .inflate(R.layout.dialog_change_name, null)
        } else {
            LayoutInflater.from(this)
                .inflate(R.layout.dialog_change_status, null)
        }

        builder.setView(view)

        dialog = builder.create()

        dialog.show()

        return view
    }

    private fun createSnackBar() {
        Snackbar.make(binding.root, "There is no internet connection", Snackbar.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && data.data != null) {
            val selectedImg = data.data!!
            binding.editImageProfile.setImageURI(selectedImg)
            val ref = storage.reference.child("profile_picture")
                .child(FirebaseAuth.getInstance().uid!!)
            ref.putFile(selectedImg).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { pic ->
                    database.reference.child("Users")
                        .child(FirebaseAuth.getInstance().uid!!).child("profilePic")
                        .setValue(pic.toString())
                }
            }
        }
    }
}