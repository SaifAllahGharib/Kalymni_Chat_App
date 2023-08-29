package com.kalymni.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kalymni.R
import com.kalymni.models.User
import de.hdodenhof.circleimageview.CircleImageView

class EditeGroupAdapter(
    val context: Context,
    private val listener: OnClickMembers,
    private val list: ArrayList<User>
) :
    RecyclerView.Adapter<EditeGroupAdapter.ViewHolder>() {

    interface OnClickMembers {
        fun onClickMember(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameOfUser = itemView.findViewById<TextView>(R.id.name_of_all_user)!!
        val newsOfUser = itemView.findViewById<TextView>(R.id.news_of_user)!!
        val image = itemView.findViewById<CircleImageView>(R.id.profile_image_simple_all_user)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.sample_all_users, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]

        Glide.with(context)
            .load(user.profilePic)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)

        holder.nameOfUser.text = user.userName
        holder.newsOfUser.text = user.status

        holder.itemView.setOnClickListener {
            listener.onClickMember(position)
        }
    }
}