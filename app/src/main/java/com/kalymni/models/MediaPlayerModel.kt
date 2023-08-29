package com.kalymni.models

import android.media.MediaPlayer

data class MediaPlayerModel(
    val mediaPlayer: MediaPlayer,
    val runnable: Runnable,
    var mediaPlayerDataSource: String? = null
)