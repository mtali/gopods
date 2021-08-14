package com.colisa.podplay.playback.players

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import timber.log.Timber

class MediaSessionCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    val mediaPlayer: MediaPlayer? = null
) : MediaSessionCompat.Callback() {

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        Timber.d("Playing ${uri.toString()}")
        onPlay()
    }

    override fun onPlay() {
        super.onPlay()
        Timber.d("onPlay()")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop()")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }


}