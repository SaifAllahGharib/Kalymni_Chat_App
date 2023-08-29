package com.kalymni.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kalymni.ChatDetailActivity
import com.kalymni.R
import com.kalymni.models.User

class UserAdapter(private var list: ArrayList<User>, var context: Context) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    private lateinit var sharedPref: SharedPreferences

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image: ImageView = itemView.findViewById(R.id.profile_image_simple_show_user)
        var userName: TextView = itemView.findViewById(R.id.name_of_user)
        var lastMessage: TextView = itemView.findViewById(R.id.lastMessage_of_user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.sample_show_users, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged", "CommitPrefEdits")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val users: User = list[position]
        sharedPref =
            context.getSharedPreferences("LastMessage", AppCompatActivity.MODE_PRIVATE)

        Glide.with(context)
            .load(users.profilePic)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)

        holder.userName.text = users.userName
        FirebaseDatabase.getInstance().reference.child("Chats")
            .child(FirebaseAuth.getInstance().uid!! + users.id).orderByChild("timeStamp")
            .limitToLast(1).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val editor = sharedPref.edit()
                    if (snapshot.exists()) {
                        if (snapshot.hasChildren()) {
                            for (snp in snapshot.children) {
                                if (snp.key != FirebaseAuth.getInstance().uid || snp.key != users.id) {
                                    holder.lastMessage.text = snp.child("message").value.toString()
                                    editor.putString(
                                        "lastMessage",
                                        snp.child("message").value.toString()
                                    )
                                    editor.apply()
                                } else {
                                    continue
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        holder.lastMessage.text = sharedPref.getString("lastMessage", "")

        holder.itemView.setOnClickListener {
            val i = Intent(context, ChatDetailActivity::class.java)
            i.putExtra("id", users.id)
            i.putExtra("name", users.userName)
            i.putExtra("profilePic", users.profilePic)
            i.putExtra("token", users.fcmToken)
            context.startActivity(i)
        }
    }
}