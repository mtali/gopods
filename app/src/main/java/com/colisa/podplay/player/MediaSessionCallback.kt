package com.colisa.podplay.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import timber.log.Timber

/**
 * Provide callbacks that you will use to create and control [MediaPlayer]
 */
class MediaSessionCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    var player: MediaPlayer? = null
) : MediaSessionCompat.Callback() {

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        Timber.d("onPlayFromUri: $uri")
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
                .build()
        )
        onPlay()
    }

    override fun onPlay() {
        super.onPlay()
        setState(PlaybackStateCompat.STATE_PLAYING)
        Timber.d("onPlay called")
    }

    override fun onStop() {
        super.onStop()
        setState(PlaybackStateCompat.STATE_STOPPED)
        Timber.d("onStop called")
    }

    override fun onPause() {
        super.onPause()
        setState(PlaybackStateCompat.STATE_PAUSED)
        Timber.d("onPause called")
    }

    private fun setState(state: Int) {
        val position: Long = -1
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, position, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }
}