@file:OptIn(DelicateCoroutinesApi::class)

package com.kalymni

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kalymni.adapters.ChatAdapter
import com.kalymni.databinding.ActivityChatDetailBinding
import com.kalymni.models.MediaPlayerModel
import com.kalymni.models.MessageModel
import com.kalymni.notification.FCMSend
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Type
import java.util.Date
import java.util.Locale
import java.util.UUID


class ChatDetailActivity : AppCompatActivity(), Timer.OnTimerTickListener,
    ChatAdapter.OnPlayOrPause {
    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var textMessage: TextInputLayout
    private lateinit var msfModel: ArrayList<MessageModel>
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var outputFile: String
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var timer: Timer
    private lateinit var amplitude: ArrayList<Float>
    private lateinit var handler: Handler
    private val mediaPlayerList: MutableList<MediaPlayerModel> = mutableListOf()
    private lateinit var duration: String
    private var isRecording: Boolean = false
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedModel: ArrayList<MessageModel>
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint(
        "NotifyDataSetChanged", "ClickableViewAccessibility", "RestrictedApi",
        "InflateParams"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        textMessage = binding.txtMessage
        timer = Timer(this)
        msfModel = ArrayList()
        handler = Handler(Looper.getMainLooper())
        sharedPref = getSharedPreferences("Messages", MODE_PRIVATE)
        editor = sharedPref.edit()

        val senderId = auth.uid
        val receiverId = intent.getStringExtra("id")
        val name = intent.getStringExtra("name")
        val profilePic = intent.getStringExtra("profilePic")
        val token = intent.getStringExtra("token")
        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId

        changeIconTextWatcher()
        loadMessageFromSharedPref()
        fetchDataFromFirebase(receiverId!!)

        coroutineScope.launch(Dispatchers.IO) {
            database.reference.child("Chats").child(senderRoom).child(receiverId.toString())
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snp in snapshot.children) {
                            if (snp.key == "typing" && snp.value == true) {
                                binding.userOnline.visibility = View.GONE
                                binding.userText.visibility = View.VISIBLE
                            } else if (snp.key == "typing" && snp.value == false) {
                                binding.userOnline.visibility = View.VISIBLE
                                binding.userText.visibility = View.GONE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        coroutineScope.launch(Dispatchers.IO) {
            database.reference.child("Users").child(receiverId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snp in snapshot.children) {
                            if (snp.key == "online" && snp.value == true) {
                                binding.userOnline.visibility = View.VISIBLE
                            } else if (snp.key == "online" && snp.value == false) {
                                binding.userOnline.visibility = View.GONE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        chatAdapter =
            ChatAdapter(this, msfModel, this, senderRoom, receiverRoom, name.toString(), "")
        binding.recyclerViewChatMessage.adapter = chatAdapter
        binding.recyclerViewChatMessage.layoutManager = LinearLayoutManager(this)
        binding.userName.text = name

        Glide.with(this)
            .load(profilePic)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.profileImage)

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.txtMessage.setStartIconOnClickListener {
            val i = Intent()
            i.action = Intent.ACTION_GET_CONTENT
            i.type = "image/*"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        i.addCategory("android.intent.category.DEFAULT")
                        i.data = Uri.parse(
                            String.format(
                                "package:%s",
                                applicationContext.packageName
                            )
                        )
                        startActivityIfNeeded(i, 101)
                    } catch (e: Exception) {
                        println(e)
                    }
                } else {
                    startActivityForResult(i, 500)
                }
            }
        }

        binding.voic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@ChatDetailActivity,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@ChatDetailActivity,
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    200
                )
            } else {
                if (NetworkStatus(this@ChatDetailActivity).isNetworkConnected()) {
                    if (!isRecording) {
                        startRecording()
                        isRecording = true
                        binding.voic.setImageResource(R.drawable.stop)
                    } else {
                        stopRecording()
                        isRecording = false
                        binding.voic.setImageResource(R.drawable.mic)
                    }
                } else {
                    createSnackBar()
                }
            }
        }

        binding.cancelVoic.setOnClickListener {
            cancelRecording()
            isRecording = false
            binding.voic.setImageResource(R.drawable.mic)
        }

        binding.send.setOnClickListener {
            val model = MessageModel(senderId, binding.txtMessage.editText!!.text.toString())
            model.type = "text"
            model.timeStamp = Date().time

            if (NetworkStatus(this@ChatDetailActivity).isNetworkConnected()) {
                GlobalScope.launch {
                    database.reference.child("Chats").child(senderRoom).push().setValue(model)
                        .addOnSuccessListener {
                            database.reference.child("Chats").child(receiverRoom).push()
                                .setValue(model)
                                .addOnSuccessListener {
                                    FCMSend.pushNotification(
                                        this@ChatDetailActivity,
                                        token.toString(),
                                        getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                                            "name",
                                            ""
                                        )!!,
                                        binding.txtMessage.editText!!.text.toString()
                                    )
                                }
                        }
                }
            } else {
                createSnackBar()
            }

            binding.txtMessage.editText!!.setText("")
            binding.recyclerViewChatMessage.scrollToPosition(msfModel.size - 1)
        }
    }

    private fun createSnackBar() {
        Snackbar.make(binding.root, "There is no internet connection", Snackbar.LENGTH_SHORT).show()
    }

    private fun changeIconTextWatcher() {
        textMessage.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textMessage.editText!!.text.toString().isNotEmpty()) {
                    binding.voic.visibility = View.GONE
                    binding.send.visibility = View.VISIBLE

                    GlobalScope.launch {
                        database.reference.child("Chats").child(receiverRoom).child(auth.uid!!)
                            .child("typing")
                            .setValue(true).addOnSuccessListener {
                                database.reference.child("Chats").child(senderRoom)
                                    .child(auth.uid!!)
                                    .child("typing").setValue(true)
                            }
                    }
                } else {
                    binding.voic.visibility = View.VISIBLE
                    binding.send.visibility = View.GONE

                    GlobalScope.launch {
                        database.reference.child("Chats").child(receiverRoom).child(auth.uid!!)
                            .child("typing")
                            .setValue(false).addOnSuccessListener {
                                database.reference.child("Chats").child(senderRoom)
                                    .child(auth.uid!!)
                                    .child("typing").setValue(false)
                            }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startRecording() {
        outputFile =
            Environment.getExternalStorageDirectory().absolutePath + "/Android/media/com.kalymni/kalymni/Recording/${System.currentTimeMillis()}.mp3"
        val file =
            File(Environment.getExternalStorageDirectory().absolutePath + "/Android/media/com.kalymni/kalymni/Recording")
        if (!file.exists()) {
            file.mkdirs()
        }
        timer.start()
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }
        binding.txtMessage.visibility = View.GONE
        binding.cancelVoic.visibility = View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        timer.stop()
        amplitude = binding.waveFromView.clear()
        binding.txtTimer.text = "0:00"
        mediaRecorder = null
        binding.txtMessage.visibility = View.VISIBLE
        binding.cancelVoic.visibility = View.GONE
        uploadAudio()
    }

    @SuppressLint("SetTextI18n")
    private fun cancelRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        timer.stop()
        binding.waveFromView.clear()
        binding.txtTimer.text = "0:00"
        amplitude = binding.waveFromView.clear()
        mediaRecorder = null

        val file = File(outputFile)
        if (file.exists()) {
            file.delete()
        }
        binding.txtTimer.text = "0:00"
        binding.txtMessage.visibility = View.VISIBLE
        binding.cancelVoic.visibility = View.GONE
    }

    private fun uploadAudio() {
        val file = File(outputFile)
        val storageReference = FirebaseStorage.getInstance().reference
        val audioRef = storageReference.child("Recording/${System.currentTimeMillis()}.mp3")

        try {
            if (file.exists()) {
                audioRef.putFile(Uri.fromFile(File(outputFile))).addOnSuccessListener { success ->
                    val audioUrl = success.storage.downloadUrl
                    audioUrl.addOnCompleteListener { path ->
                        if (path.isSuccessful) {
                            val model = MessageModel(auth.uid, path.result.toString())
                            model.type = "recording"
                            model.timeStamp = Date().time
                            model.imageForSender =
                                getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                                    "image",
                                    ""
                                )
                            model.nameForSender = intent.getStringExtra("name")
                            database.reference.child("Chats").child(senderRoom).push()
                                .setValue(model)
                                .addOnSuccessListener {
                                    database.reference.child("Chats").child(receiverRoom).push()
                                        .setValue(model)
                                }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("audio Ex: $e")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@ChatDetailActivity,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                200
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 500 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImage = data.data
            val selectedImageBmp = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            val outputStream = ByteArrayOutputStream()
            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val selectedIMageBytes = outputStream.toByteArray()
            uploadMessageImage(selectedIMageBytes) { imagePath ->
                imagePath.addOnSuccessListener { pic ->
                    val model = MessageModel(auth.uid, pic.toString(), Date().time, "image")
                    database.reference.child("Chats")
                        .child(senderRoom).push().setValue(model).addOnSuccessListener {
                            database.reference.child("Chats")
                                .child(receiverRoom).push().setValue(model)
                        }
                }
            }
        }
    }

    private fun uploadMessageImage(
        imageByte: ByteArray,
        onSuccess: (imagePath: Task<Uri>) -> Unit
    ) {
        val ref = FirebaseStorage.getInstance().reference.child("imagesForChats")
            .child("$senderRoom/${UUID.nameUUIDFromBytes(imageByte)}")
        ref.putBytes(imageByte).addOnSuccessListener {
            onSuccess(ref.downloadUrl)
        }
    }

    private fun loadMessageFromSharedPref() {
        coroutineScope.launch(Dispatchers.IO) {
            msfModel.clear()
            val gson = Gson()
            val json = sharedPref.getString("messageUsers", "")
            val type: Type = object : TypeToken<ArrayList<MessageModel>>() {
            }.type
            sharedModel = if (!json.isNullOrEmpty()) {
                gson.fromJson(json, type)
            } else {
                ArrayList()
            }

            msfModel.addAll(sharedModel)

        }
    }

    private fun fetchDataFromFirebase(receiverId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            database.reference.child("Chats").child(senderRoom)
                .addValueEventListener(object : ValueEventListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        msfModel.clear()
                        sharedModel.clear()
                        for (snp in snapshot.children) {
                            if (snp.key != auth.uid && snp.key != receiverId) {
                                val messageModel = snp.getValue(MessageModel::class.java)!!
                                sharedModel.add(messageModel)
                            } else {
                                continue
                            }
                        }
                        val gson = Gson()
                        val json = gson.toJson(sharedModel)
                        editor.remove("messageUsers")
                        editor.apply()
                        editor.putString("messageUsers", json)
                        editor.apply()
                        loadMessageFromSharedPref()
                        binding.recyclerViewChatMessage.scrollToPosition(msfModel.size - 1)
                        chatAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
        }
    }

    override fun onTimerTick(duration: String) {
        binding.txtTimer.text = duration
        this.duration = duration
        binding.waveFromView.addAmplitude((mediaRecorder!!.maxAmplitude).toFloat())
    }

    @SuppressLint("SetTextI18n")
    override fun playOrPauseSender(
        seekbar: SeekBar,
        imageSenderRecord: CircleImageView,
        btnPlay: ImageView,
        currentTimeOfRecord: TextView,
        msg: String,
        imageMsg: String
    ) {
        createMediaPlayer(
            msg,
            seekbar,
            currentTimeOfRecord,
            btnPlay
        )

        val currentMediaPlayerModel = mediaPlayerList.find { it.mediaPlayerDataSource == msg }!!
        val currentMediaPlayer = currentMediaPlayerModel.mediaPlayer
        val currentRunnable = currentMediaPlayerModel.runnable

        btnPlay.setOnClickListener {
            if (currentMediaPlayer.isPlaying) {
                handler.removeCallbacks(currentRunnable)
                currentMediaPlayer.pause()
            } else {
                currentMediaPlayer.start()
                try {
                    updateSeekbar(
                        currentMediaPlayer,
                        seekbar,
                        currentTimeOfRecord,
                        currentRunnable
                    )
                } catch (e: Exception) {
                    println("Error Seekbar: $e")
                    println("Error Message Seekbar: ${e.message}")
                }
            }

            if (currentMediaPlayer.isPlaying) {
                btnPlay.setImageResource(R.drawable.pause)
            } else {
                btnPlay.setImageResource(R.drawable.play_arrow)
            }
        }

        Glide.with(this)
            .load(imageMsg)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageSenderRecord)
    }

    override fun playOrPauseReceiver(
        seekbar: SeekBar,
        imageReceiverRecord: CircleImageView,
        btnPlay: ImageView,
        currentTimeOfRecord: TextView,
        msg: String,
        imageMsg: String
    ) {
        createMediaPlayer(
            msg,
            seekbar,
            currentTimeOfRecord,
            btnPlay
        )

        val currentMediaPlayerModel = mediaPlayerList.find { it.mediaPlayerDataSource == msg }!!
        val currentMediaPlayer = currentMediaPlayerModel.mediaPlayer
        val currentRunnable = currentMediaPlayerModel.runnable

        btnPlay.setOnClickListener {
            if (currentMediaPlayer.isPlaying) {
                handler.removeCallbacks(currentRunnable)
                currentMediaPlayer.pause()
            } else {
                currentMediaPlayer.start()
                println("mediaPlayerDataSource: ${currentMediaPlayerModel.mediaPlayerDataSource}")
                try {
                    updateSeekbar(
                        currentMediaPlayer,
                        seekbar,
                        currentTimeOfRecord,
                        currentRunnable
                    )
                } catch (e: Exception) {
                    println("Error Seekbar: $e")
                    println("Error Message Seekbar: ${e.message}")
                }
            }

            if (currentMediaPlayer.isPlaying) {
                btnPlay.setImageResource(R.drawable.pause)
            } else {
                btnPlay.setImageResource(R.drawable.play_arrow)
            }
        }

        Glide.with(this)
            .load(imageMsg)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageReceiverRecord)
    }

    private fun updateSeekbar(
        mediaPlayer: MediaPlayer,
        seekbar: SeekBar,
        currentTimeOfRecord: TextView,
        runnable: Runnable
    ) {
        if (mediaPlayer.isPlaying) {
            seekbar.progress = mediaPlayer.currentPosition
            handler.postDelayed({
                updateSeekbar(mediaPlayer, seekbar, currentTimeOfRecord, runnable)
                val currentDuration = mediaPlayer.currentPosition
                currentTimeOfRecord.text = format(currentDuration)
            }, 0)
        }
    }

    private fun createMediaPlayer(
        dataSource: String,
        seekbar: SeekBar,
        currentTimeOfRecord: TextView,
        btnPlay: ImageView
    ): MediaPlayer {
        val mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(dataSource)
            mediaPlayer.prepare()
        } catch (e: Exception) {
            println("Error: $e")
            println("Error Message: ${e.message}")
        }

        currentTimeOfRecord.text = format(mediaPlayer.duration)
        seekbar.max = mediaPlayer.duration

        val runnable = Runnable {
            val currentDuration = mediaPlayer.currentPosition
            currentTimeOfRecord.text = format(currentDuration)
        }

        mediaPlayer.setOnCompletionListener {
            btnPlay.setImageResource(R.drawable.play_arrow)
            seekbar.progress = 0
            currentTimeOfRecord.text = format(mediaPlayer.duration)
        }

        val mediaPlayerModel = MediaPlayerModel(mediaPlayer, runnable, dataSource)
        mediaPlayerList.add(mediaPlayerModel)

        return mediaPlayer
    }

    private fun format(duration: Int): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return if (hours > 0) {
            "${String.format(Locale.ENGLISH, "%01d", hours)}:${
                String.format(
                    Locale.ENGLISH,
                    "%02d",
                    minutes
                )
            }:${String.format(Locale.ENGLISH, "%02d", seconds)}"
        } else {
            "${String.format(Locale.ENGLISH, "%01d", minutes)}:${
                String.format(
                    Locale.ENGLISH,
                    "%02d",
                    seconds
                )
            }"
        }
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
