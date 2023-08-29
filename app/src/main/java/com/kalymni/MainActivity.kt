package com.kalymni

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.kalymni.adapters.FragmentAdapter
import com.kalymni.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.elevation = 0.0F

        auth = FirebaseAuth.getInstance()
        sharedPref =
            applicationContext.getSharedPreferences("signing", MODE_PRIVATE)
        editor = sharedPref.edit()

        binding.viewPagerHomeScreen.adapter = FragmentAdapter(supportFragmentManager)
        binding.tabeLayoutHomeScreen.setupWithViewPager(binding.viewPagerHomeScreen)

        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("online")
            .setValue(true)

        getFCMToken()
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseDatabase.getInstance().reference.child("Users").child(auth.uid!!)
                    .child("fcmToken").setValue(task.result)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflate: MenuInflater = menuInflater
        inflate.inflate(R.menu.menu_chat_fregmant, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chat_room -> {
                startActivity(Intent(this@MainActivity, SelectUserToGroup::class.java))
            }

            R.id.settings -> {
                startActivity(Intent(this@MainActivity, EditProfile::class.java))
            }

            R.id.logOut -> {
                auth.signOut()
                editor.remove("signed")
                editor.apply()
                startActivity(Intent(this@MainActivity, SignIn::class.java))
                finish()
            }
        }
        return true
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