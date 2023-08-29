package com.kalymni

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.kalymni.databinding.ActivitySignInBinding

class SignIn : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sharedPref =
            applicationContext.getSharedPreferences("signing", MODE_PRIVATE)

        binding.btnContinue.setOnClickListener {
            if (binding.phoneNumber.editText!!.text.isEmpty() || binding.code.editText!!.text.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                if (binding.phoneNumber.editText!!.text.isEmpty()) {
                    builder.setMessage("Please enter the phone number.")
                } else if (binding.code.editText!!.text.isEmpty()) {
                    builder.setMessage("Please enter your country code.")
                }
                builder.setCancelable(false)
                builder.setPositiveButton("ok") { d, _ ->
                    d.cancel()
                }
                val alertDialog = builder.create()
                alertDialog.show()
            } else {
                var allCode = ""
                if (binding.code.editText!!.text.toString() == "20") {
                    for (n in binding.code.editText!!.text) {
                        if (n == '0') {
                            continue
                        } else {
                            allCode += n
                        }
                    }
                }
                val i = Intent(this, VerifyPhoneNumber::class.java)
                i.putExtra(
                    "phone",
                    "+" + allCode + binding.phoneNumber.editText!!.text.toString()
                )
                startActivity(i)
                finish()
            }
        }

        if (sharedPref.getBoolean("signed", false)) {
            startActivity(Intent(this@SignIn, MainActivity::class.java))
            finish()
        }
    }
}