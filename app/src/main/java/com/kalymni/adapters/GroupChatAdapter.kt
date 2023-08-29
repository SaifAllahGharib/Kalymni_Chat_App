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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kalymni.GroupChatActivity
import com.kalymni.R
import com.kalymni.models.GroupModel

class GroupChatAdapter(val context: Context, private val list: ArrayList<GroupModel>) :
    RecyclerView.Adapter<GroupChatAdapter.ViewHolder>() {

    private lateinit var sharedPref: SharedPreferences

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        var imageGroup: ImageView = itemView.findViewById(R.id.image_group)
        var nameGroup: TextView = itemView.findViewById(R.id.name_group)
        var lastMessageGroup: TextView = itemView.findViewById(R.id.lastMessage_group)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.group_chat_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val groupModel = list[position]

        sharedPref =
            context.getSharedPreferences("LastMessageGroup", AppCompatActivity.MODE_PRIVATE)

        Glide.with(context)
            .load(groupModel.image)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.imageGroup)

        holder.nameGroup.text = groupModel.name

        FirebaseDatabase.getInstance().reference.child("Groups").child(groupModel.id.toString())
            .child("ChatList").orderByChild("timeStamp")
            .limitToLast(1).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val editor = sharedPref.edit()
                    if (snapshot.hasChildren()) {
                        for (snp in snapshot.children) {
                            holder.lastMessageGroup.text = snp.child("message").value.toString()
                            editor.putString(
                                "lastMessageGroup",
                                snp.child("message").value.toString()
                            )
                            editor.apply()
                        }
                    } else {
                        holder.lastMessageGroup.text = "Here is the last message"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        holder.lastMessageGroup.text = sharedPref.getString("lastMessageGroup", "")

        holder.itemView.setOnClickListener {
            val i = Intent(
                context,
                GroupChatActivity::class.java
            )

            i.putExtra("idGroup", groupModel.id)
            i.putExtra("nameGroup", groupModel.name)
            i.putExtra("imageGroup", groupModel.image)
            i.putExtra("adminIdGroup", groupModel.adminId)
            context.startActivity(i)
        }
    }
}