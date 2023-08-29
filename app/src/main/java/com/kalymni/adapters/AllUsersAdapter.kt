package com.kalymni.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kalymni.ChatDetailActivity
import com.kalymni.R
import com.kalymni.models.User
import de.hdodenhof.circleimageview.CircleImageView

class AllUsersAdapter(
    val context: Activity,
    private val list: ArrayList<User>
) :
    RecyclerView.Adapter<AllUsersAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameOfUser = itemView.findViewById<TextView>(R.id.name_of_all_user)!!
        val newsOfUser = itemView.findViewById<TextView>(R.id.news_of_user)!!
        val image = itemView.findViewById<CircleImageView>(R.id.profile_image_simple_all_user)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sample_all_users, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: User = list[position]

        Glide.with(context)
            .load(model.profilePic)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)

        holder.nameOfUser.text = model.userName
        holder.newsOfUser.text = model.status
        holder.itemView.setOnClickListener {
            val i = Intent(context, ChatDetailActivity::class.java)
            i.putExtra("id", model.id)
            i.putExtra("name", model.userName)
            i.putExtra("profilePic", model.profilePic)
            context.startActivity(i)
            context.finish()
        }
    }
}