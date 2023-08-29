package com.kalymni.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kalymni.R
import com.kalymni.models.User

class UserGroupAdapter(
    private var context: Context,
    private var list: ArrayList<User>,
    private var layoutRes: Int,
    private var showImageOnly: Boolean,
    private var move: (position: Int) -> Unit
) :
    RecyclerView.Adapter<UserGroupAdapter.ViewHolder>() {

    class ViewHolder(itemView: View? = null) : RecyclerView.ViewHolder(itemView!!) {
        var image: ImageView = itemView!!.findViewById(R.id.profile_image_simple_show_user)
        var userName: TextView = itemView!!.findViewById(R.id.name_of_user)
        var news: TextView = itemView!!.findViewById(R.id.lastMessage_of_user)
        var check: RelativeLayout = itemView!!.findViewById(R.id.check)
        var isChecked = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("NotifyDataSetChanged", "InflateParams", "CutPasteId")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user: User = list[position]
        
        Glide.with(context)
            .load(user.profilePic)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.image)

        if (showImageOnly) {
            holder.userName.text = user.userName
            holder.news.visibility = View.GONE
            holder.check.visibility = View.GONE
        } else {
            holder.userName.text = user.userName
            holder.news.text = user.status
        }

        holder.itemView.setOnClickListener {
            if (layoutRes == R.layout.sample_show_users) {
                if (!holder.isChecked) {
                    holder.isChecked = true
                    holder.check.visibility = View.VISIBLE
                } else {
                    holder.isChecked = false
                    holder.check.visibility = View.GONE
                }
            }
            move(position)
        }
    }
}