package com.kalymni.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kalymni.NetworkStatus
import com.kalymni.R
import com.kalymni.models.MessageModel
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ChatAdapter(
    var context: Context,
    private var msgModel: ArrayList<MessageModel>,
    private var listener: OnPlayOrPause,
    private var senderRoom: String?,
    private var receiverRoom: String?,
    private var name: String?,
    private var groupId: String?,
    private var isGroup: Boolean = false
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val SENDER_VIEW_TYPE = 1
    val RECEIVER_VIEW_TYPE = 2
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    interface OnPlayOrPause {
        fun playOrPauseSender(
            seekbar: SeekBar,
            imageSenderRecord: CircleImageView,
            btnPlay: ImageView,
            currentTimeOfRecord: TextView,
            msg: String,
            imageMsg: String
        )

        fun playOrPauseReceiver(
            seekbar: SeekBar,
            imageReceiverRecord: CircleImageView,
            btnPlay: ImageView,
            currentTimeOfRecord: TextView,
            msg: String,
            imageMsg: String
        )
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            SENDER_VIEW_TYPE -> {
                val view =
                    LayoutInflater.from(context).inflate(R.layout.simple_sender, parent, false)
                return SenderViewHolder(view)
            }

            RECEIVER_VIEW_TYPE -> {
                val view = if (!isGroup) {
                    LayoutInflater.from(context).inflate(R.layout.simple_reciever, parent, false)
                } else {
                    LayoutInflater.from(context)
                        .inflate(R.layout.simple_reciever_group, parent, false)
                }
                return ReceiverViewHolder(view, isGroup)
            }

            100 -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.sender_recording_message, parent, false)
                return SenderRecordingViewHolder(view)
            }

            200 -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.receiver_recording_message, parent, false)
                return ReceiverRecordingViewHolder(view)
            }

            300 -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.image_sender, parent, false)
                return SenderImageViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.image_receiver, parent, false)
                return ReceiverImageViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (msgModel[position].uId == FirebaseAuth.getInstance().uid) {
            when (msgModel[position].type) {
                "text" -> {
                    SENDER_VIEW_TYPE
                }

                "recording" -> {
                    100
                }

                else -> {
                    300
                }
            }
        } else {
            when (msgModel[position].type) {
                "text" -> {
                    RECEIVER_VIEW_TYPE
                }

                "recording" -> {
                    200
                }

                else -> {
                    400
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return msgModel.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msgModel = msgModel[position]
        when (holder::class.java) {
            SenderViewHolder::class.java -> {
                ((holder) as SenderViewHolder).senderMsg.text = msgModel.message
                holder.senderTime.text =
                    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(msgModel.timeStamp!!))

                if (!isGroup) {
                    deleteMessage(holder, msgModel)
                } else {
                    deleteMessageFromGroup(holder, msgModel)
                }
            }

            ReceiverViewHolder::class.java -> {
                ((holder) as ReceiverViewHolder).receiverMsg.text = msgModel.message
                holder.receiverTime.text =
                    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(msgModel.timeStamp!!))
                if (holder.receiverName != null) {
                    holder.receiverName!!.text = msgModel.nameForSender.toString()
                }

                if (isGroup) {
//                    Glide.with(context)
//                        .load(msgModel.imageForSender)
//                        .apply(RequestOptions.placeholderOf(R.drawable.person))
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.receiverImage!!)

                    coroutineScope.launch(Dispatchers.IO) {
                        loadImgForMsg(
                            msgModel.imageForSender.toString(),
                            holder.receiverImage!!
                        )
                    }
                }
            }

            ReceiverRecordingViewHolder::class.java -> {
                ((holder) as ReceiverRecordingViewHolder).receiverTimeRecord.text =
                    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(msgModel.timeStamp!!))

                listener.playOrPauseReceiver(
                    holder.seekbarReceiver,
                    holder.imageReceiverRecord,
                    holder.btnPlay,
                    holder.currentTimeOfRecord,
                    msgModel.message.toString(),
                    msgModel.imageForSender.toString()
                )
            }

            SenderRecordingViewHolder::class.java -> {
                ((holder) as SenderRecordingViewHolder).senderTime.text =
                    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(msgModel.timeStamp!!))

                listener.playOrPauseSender(
                    holder.seekbar,
                    holder.imageSenderRecord,
                    holder.btnPlay,
                    holder.currentTimeOfRecord,
                    msgModel.message.toString(),
                    msgModel.imageForSender.toString()
                )

                if (!isGroup) {
                    deleteMessage(holder, msgModel)
                } else {
                    deleteMessageFromGroup(holder, msgModel)
                }
            }

            SenderImageViewHolder::class.java -> {
                ((holder) as SenderImageViewHolder).senderTime.text =
                    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(msgModel.timeStamp!!))

//                Glide.with(context)
//                    .load(msgModel.message)
//                    .apply(RequestOptions.placeholderOf(R.drawable.person))
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.senderImage)

                coroutineScope.launch(Dispatchers.IO) {
                    loadMsgImg(
                        msgModel.message.toString(),
                        holder.senderImage
                    )
                }

                if (!isGroup) {
                    deleteMessage(holder, msgModel)
                } else {
                    deleteMessageFromGroup(holder, msgModel)
                }
            }

            ReceiverImageViewHolder::class.java -> {
                ((holder) as ReceiverImageViewHolder)

                holder.receiverTimeImage.text =
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.ENGLISH
                    ).format(Date(msgModel.timeStamp!!))

//                Glide.with(context)
//                    .load(msgModel.message)
//                    .apply(RequestOptions.placeholderOf(R.drawable.person))
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.receiverImage)

                coroutineScope.launch(Dispatchers.IO) {
                    loadMsgImg(
                        msgModel.message.toString(),
                        holder.receiverImage
                    )
                }

//                Glide.with(context)
//                    .load(msgModel.imageForSender)
//                    .apply(RequestOptions.placeholderOf(R.drawable.person))
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.msgImage)

                coroutineScope.launch(Dispatchers.IO) {
                    loadImgForMsg(
                        msgModel.imageForSender.toString(),
                        holder.msgImage
                    )
                }
            }
        }
    }

    class ReceiverViewHolder(itemView: View, isGroup: Boolean) : RecyclerView.ViewHolder(itemView) {
        var receiverMsg: TextView
        var receiverTime: TextView
        var receiverName: TextView?
        var receiverImage: CircleImageView?

        init {
            receiverMsg = if (!isGroup) {
                itemView.findViewById(R.id.msg_receiver)
            } else {
                itemView.findViewById(R.id.msg_receiver_group)
            }

            receiverTime = if (!isGroup) {
                itemView.findViewById(R.id.time_msg_receiver)
            } else {
                itemView.findViewById(R.id.time_msg_receiver_group)
            }

            receiverName = if (isGroup) {
                itemView.findViewById(R.id.nameOfUser_receiver_group)
            } else {
                null
            }

            receiverImage = if (isGroup) {
                itemView.findViewById(R.id.profile_image_msg_receiver_group)
            } else {
                null
            }
        }
    }

    class SenderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var senderMsg: TextView = itemView.findViewById(R.id.msg_sender)
        var senderTime: TextView = itemView.findViewById(R.id.time_msg_sender)
    }

    class ReceiverRecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var receiverTimeRecord: TextView
        var currentTimeOfRecord: TextView
        var btnPlay: ImageView
        var imageReceiverRecord: CircleImageView
        var seekbarReceiver: SeekBar

        init {
            receiverTimeRecord = itemView.findViewById(R.id.time_recording_receiver)
            currentTimeOfRecord = itemView.findViewById(R.id.count_time_recording_message_receiver)
            btnPlay = itemView.findViewById(R.id.btn_play_receiver)
            imageReceiverRecord =
                itemView.findViewById(R.id.profile_image_recording_message_receiver)
            seekbarReceiver = itemView.findViewById(R.id.track_play_voice_receiver)
        }
    }

    class SenderRecordingViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        lateinit var senderTime: TextView
        lateinit var currentTimeOfRecord: TextView
        lateinit var btnPlay: ImageView
        lateinit var imageSenderRecord: CircleImageView
        lateinit var seekbar: SeekBar

        init {
            if (itemView != null) {
                senderTime = itemView.findViewById(R.id.time_recording_sender)
                currentTimeOfRecord = itemView.findViewById(R.id.count_time_recording_message)
                btnPlay = itemView.findViewById(R.id.btn_play)
                imageSenderRecord =
                    itemView.findViewById(R.id.profile_image_recording_message)
                seekbar = itemView.findViewById(R.id.track_play_voice)
            }
        }
    }

    class SenderImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderImage: ImageView = itemView.findViewById(R.id.image_sender)
        val senderTime: TextView = itemView.findViewById(R.id.time_image_sender)
    }

    class ReceiverImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverImage: ImageView = itemView.findViewById(R.id.image_receiver)
        val receiverTimeImage: TextView = itemView.findViewById(R.id.time_image_receiver)
        val msgImage: CircleImageView = itemView.findViewById(R.id.msgImage_receiver_group)
    }

    private suspend fun loadMsgImg(url: String, imageView: ImageView) {
        try {
            val requestOptions = RequestOptions
                .placeholderOf(R.drawable.person)
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            val bitmap = Glide.with(context)
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

    private suspend fun loadImgForMsg(url: String, imageView: CircleImageView) {
        try {
            val requestOptions = RequestOptions
                .placeholderOf(R.drawable.person)
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            val bitmap = Glide.with(context)
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

    private fun deleteMessage(holder: RecyclerView.ViewHolder, msgModel: MessageModel) {
        holder.itemView.setOnLongClickListener {
            FirebaseDatabase.getInstance().reference.child("Chats").child(senderRoom!!)
                .addValueEventListener(object : ValueEventListener {
                    @SuppressLint("MissingInflatedId", "SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snp in snapshot.children) {
                            for (s in snp.children) {
                                if (s.key == "timeStamp" && s.value == msgModel.timeStamp) {
                                    val builder = AlertDialog.Builder(context)
                                    val view = LayoutInflater.from(context)
                                        .inflate(R.layout.dialog_delete_message, null)
                                    builder.setView(view)

                                    val dialog = builder.create()

                                    view.findViewById<TextView>(R.id.nameForDeleteMessage).text =
                                        "Delete message from $name?"

                                    view.findViewById<Button>(R.id.btnDeleteFromMe)
                                        .setOnClickListener {
                                            if (NetworkStatus(context).isNetworkConnected()) {
                                                FirebaseDatabase.getInstance().reference.child("Chats")
                                                    .child(senderRoom!!).child(snp.key.toString())
                                                    .removeValue().addOnSuccessListener {
                                                        receiverRoom?.let { it1 ->
                                                            FirebaseDatabase.getInstance().reference.child(
                                                                "Chats"
                                                            )
                                                                .child(it1)
                                                                .addValueEventListener(object :
                                                                    ValueEventListener {
                                                                    @SuppressLint(
                                                                        "MissingInflatedId",
                                                                        "SetTextI18n"
                                                                    )
                                                                    override fun onDataChange(
                                                                        snapshot: DataSnapshot
                                                                    ) {
                                                                        for (snp in snapshot.children) {
                                                                            for (s in snp.children) {
                                                                                if (s.key == "timeStamp" && s.value == msgModel.timeStamp) {
                                                                                    FirebaseDatabase.getInstance().reference.child(
                                                                                        "Chats"
                                                                                    )
                                                                                        .child(
                                                                                            receiverRoom!!
                                                                                        )
                                                                                        .child(snp.key.toString())
                                                                                        .removeValue()
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    override fun onCancelled(error: DatabaseError) {}
                                                                })
                                                        }
                                                    }
                                            } else {
                                                createToast()
                                            }

                                            dialog.dismiss()
                                        }

                                    view.findViewById<Button>(R.id.btnCancelDeleteMessage)
                                        .setOnClickListener {
                                            dialog.dismiss()
                                        }

                                    dialog.show()
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            false
        }
    }

    private fun deleteMessageFromGroup(holder: RecyclerView.ViewHolder, msgModel: MessageModel) {
        holder.itemView.setOnLongClickListener {
            FirebaseDatabase.getInstance().reference.child("Groups").child(groupId!!)
                .child("ChatList")
                .addValueEventListener(object : ValueEventListener {
                    @SuppressLint("MissingInflatedId", "SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snp in snapshot.children) {
                            for (s in snp.children) {
                                if (s.key == "timeStamp" && s.value == msgModel.timeStamp) {
                                    println("snp: $snp")
                                    val builder = AlertDialog.Builder(context)
                                    val view = LayoutInflater.from(context)
                                        .inflate(R.layout.dialog_delete_message, null)
                                    builder.setView(view)

                                    val dialog = builder.create()

                                    view.findViewById<TextView>(R.id.nameForDeleteMessage).text =
                                        "Delete message from $name?"

                                    view.findViewById<Button>(R.id.btnDeleteFromMe)
                                        .setOnClickListener {
                                            if (NetworkStatus(context).isNetworkConnected()) {
                                                FirebaseDatabase.getInstance().reference.child("Groups")
                                                    .child(groupId!!)
                                                    .child("ChatList").child(snp.key.toString())
                                                    .removeValue()
                                            } else {
                                                createToast()
                                            }
                                            dialog.dismiss()
                                        }

                                    view.findViewById<Button>(R.id.btnCancelDeleteMessage)
                                        .setOnClickListener {
                                            dialog.dismiss()
                                        }

                                    dialog.show()
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            false
        }
    }

    private fun createToast() {
        Toast.makeText(context, "There is no internet connection", Toast.LENGTH_SHORT).show()
    }
}