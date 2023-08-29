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
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import com.kalymni.databinding.ActivityGroupChatBinding
import com.kalymni.models.GroupModel
import com.kalymni.models.MediaPlayerModel
import com.kalymni.models.MessageModel
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Type
import java.util.Date
import java.util.Locale
import java.util.UUID

class GroupChatActivity : AppCompatActivity(), ChatAdapter.OnPlayOrPause,
    Timer.OnTimerTickListener {
    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var textMessage: TextInputLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedModel: ArrayList<MessageModel>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var sharedPrefGroupRole: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var adapter: ChatAdapter
    private lateinit var msgModel: ArrayList<MessageModel>
    private lateinit var idGroup: String
    private lateinit var nameGroup: String
    private lateinit var imageGroup: String
    private lateinit var outputFile: String
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var timer: Timer
    private lateinit var duration: String
    private lateinit var amplitude: ArrayList<Float>
    private lateinit var handler: Handler
    private val mediaPlayerList: MutableList<MediaPlayerModel> = mutableListOf()
    private var isRecording: Boolean = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var toolBar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        textMessage = binding.txtMessage
        toolBar = binding.toolBarChatDetailsGroup
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        timer = Timer(this)
        sharedPref = applicationContext.getSharedPreferences("MESSAGE_GROUP_USERS", MODE_PRIVATE)
        sharedPrefGroupRole = applicationContext.getSharedPreferences("GROUP_ROLE", MODE_PRIVATE)
        editor = sharedPref.edit()
        msgModel = ArrayList()
        handler = Handler(Looper.getMainLooper())
        idGroup = intent.getStringExtra("idGroup").toString()
        nameGroup = intent.getStringExtra("nameGroup").toString()
        imageGroup = intent.getStringExtra("imageGroup").toString()
        val adminIdGroup = intent.getStringExtra("adminIdGroup").toString()

        coroutineScope.launch(Dispatchers.IO) {
            database.reference.child("Groups").child(idGroup)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val group = snapshot.getValue(GroupModel::class.java)!!
                            println("group : $group")
                            for ((k, v) in group.Members!!.entries) {
                                if (k == auth.uid) {
                                    if (v.role == "admin") {
                                        toolBar.inflateMenu(R.menu.menu_chat_activity_admin)
                                        sharedPrefGroupRole.edit().putString("role", "admin")
                                            .apply()
                                    } else if (v.role == "member") {
                                        toolBar.inflateMenu(R.menu.menu_chat_activity_members)
                                        sharedPrefGroupRole.edit().putString("role", "member")
                                            .apply()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println(e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        loadData()
        fetchDataFromFirebase()

        Glide.with(this)
            .load(imageGroup)
            .apply(RequestOptions.placeholderOf(R.drawable.person))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.profileImageGroup)

        binding.userNameGroup.text = nameGroup

        textMessage.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (textMessage.editText!!.text.toString().isNotEmpty()) {
                    binding.voic.visibility = View.GONE
                    binding.send.visibility = View.VISIBLE
                } else {
                    binding.voic.visibility = View.VISIBLE
                    binding.send.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.backGroup.setOnClickListener {
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
                    this@GroupChatActivity,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@GroupChatActivity,
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    200
                )
            } else {
                if (NetworkStatus(this@GroupChatActivity).isNetworkConnected()) {
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

        adapter = ChatAdapter(
            this,
            msgModel,
            this,
            null,
            null,
            getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                "name",
                ""
            ),
            idGroup,
            true
        )
        binding.recyclerViewChatMessageGroup.adapter = adapter
        binding.recyclerViewChatMessageGroup.layoutManager = LinearLayoutManager(this)

        binding.cancelVoic.setOnClickListener {
            cancelRecording()
            isRecording = false
            binding.voic.setImageResource(R.drawable.mic)
        }

        binding.send.setOnClickListener {
            val model = MessageModel(auth.uid, binding.txtMessage.editText!!.text.toString())
            model.type = "text"
            model.timeStamp = Date().time
            model.nameForSender = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                "name",
                ""
            )
            model.imageForSender =
                getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                    "image",
                    ""
                )
            binding.txtMessage.editText!!.setText("")

            if (!NetworkStatus(this@GroupChatActivity).isNetworkConnected()) {
                loadData()
            }

            if (NetworkStatus(this@GroupChatActivity).isNetworkConnected()) {
                database.reference.child("Groups").child(idGroup).child("ChatList").push()
                    .setValue(model)
            } else {
                createSnackBar()
            }
        }

        toolBar.setOnMenuItemClickListener {
            if (sharedPrefGroupRole.getString("role", "") == "admin") {
                if (NetworkStatus(this).isNetworkConnected()) {
                    when (it.itemId) {
                        R.id.settingsGroup -> {
                            val i = Intent(
                                this,
                                EditeGroup::class.java
                            )

                            i.putExtra("idGroup", idGroup)
                            i.putExtra("imageGroup", imageGroup)
                            i.putExtra("nameGroup", nameGroup)
                            startActivity(i)
                            finish()
                        }

                        R.id.leave_group -> {
                            removeMember()
                        }
                    }
                } else {
                    createSnackBar()
                }
            } else {
                if (NetworkStatus(this).isNetworkConnected()) {
                    when (it.itemId) {
                        R.id.leave_group -> {
                            removeMember()
                        }
                    }
                } else {
                    createSnackBar()
                }
            }

            true
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (sharedPrefGroupRole.getString("role", "") == "admin") {
            menuInflater.inflate(R.menu.menu_chat_activity_admin, menu)
        } else {
            menuInflater.inflate(R.menu.menu_chat_activity_members, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun createSnackBar() {
        Snackbar.make(binding.root, "There is no internet connection", Snackbar.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingInflatedId")
    private fun removeMember() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_remove_member, null)
        builder.setView(view)

        val alert = builder.create()
        alert.show()

        view.findViewById<Button>(R.id.yes).setOnClickListener {
            database.reference.child("Groups")
                .child(intent.getStringExtra("idGroup").toString())
                .child("Members").child(auth.uid.toString()).removeValue()
        }

        view.findViewById<Button>(R.id.no).setOnClickListener {
            alert.dismiss()
        }
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
        val storageReference = FirebaseStorage.getInstance().reference
        val audioRef = storageReference.child("RecordingGroups/${System.currentTimeMillis()}.mp3")
        audioRef.putFile(Uri.fromFile(File(outputFile))).addOnSuccessListener { success ->
            val audioUrl = success.storage.downloadUrl
            audioUrl.addOnCompleteListener { path ->
                if (path.isSuccessful) {
                    val model = MessageModel(auth.uid, path.result.toString())
                    model.type = "recording"
                    model.nameForSender = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                        "name",
                        ""
                    )
                    model.imageForSender =
                        getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                            "image",
                            ""
                        )
                    model.timeStamp = Date().time
                    database.reference.child("Groups").child(idGroup).child("ChatList").push()
                        .setValue(model)
                }
            }
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
                this@GroupChatActivity,
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
                    model.nameForSender = getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                        "name",
                        ""
                    )
                    model.imageForSender =
                        getSharedPreferences("USER_INFO", MODE_PRIVATE).getString(
                            "image",
                            ""
                        )
                    database.reference.child("Groups").child(idGroup).child("ChatList").push()
                        .setValue(model)
                }
            }
        }
    }

    private fun uploadMessageImage(
        imageByte: ByteArray,
        onSuccess: (imagePath: Task<Uri>) -> Unit
    ) {
        val ref = FirebaseStorage.getInstance().reference.child("imagesForChats")
            .child("$idGroup/${UUID.nameUUIDFromBytes(imageByte)}")
        ref.putBytes(imageByte).addOnSuccessListener {
            onSuccess(ref.downloadUrl)
        }
    }

    private fun loadData() {
        coroutineScope.launch(Dispatchers.IO) {
            msgModel.clear()
            val gson = Gson()
            val json = sharedPref.getString("messageGroupUsers", "")
            val type: Type = object : TypeToken<ArrayList<MessageModel>>() {
            }.type
            sharedModel = if (!json.isNullOrEmpty()) {
                gson.fromJson(json, type)
            } else {
                ArrayList()
            }

            binding.recyclerViewChatMessageGroup.scrollToPosition(sharedModel.size - 1)

            msgModel.addAll(sharedModel)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchDataFromFirebase() {
        coroutineScope.launch(Dispatchers.IO) {
            database.reference.child("Groups").child(idGroup).child("ChatList")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        msgModel.clear()
                        sharedModel.clear()
                        for (snp in snapshot.children) {
                            val messageModel = snp.getValue(MessageModel::class.java)!!
                            sharedModel.add(messageModel)
                        }
                        val gson = Gson()
                        val json = gson.toJson(sharedModel)
                        editor.remove("messageGroupUsers")
                        editor.apply()
                        editor.putString("messageGroupUsers", json)
                        editor.apply()
                        loadData()
                        adapter.notifyDataSetChanged()
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

    override fun onStop() {
        super.onStop()

        coroutineScope.cancel()
    }
}