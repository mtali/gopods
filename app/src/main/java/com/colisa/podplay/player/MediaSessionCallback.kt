package com.colisa.podplay.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.colisa.podplay.extensions.getAudioManager
import com.colisa.podplay.util.VersionUtils
import timber.log.Timber

/**
 * Provide callbacks that you will use to create and control [MediaPlayer]
 */
class MediaSessionCallback(
    private val context: Context,
    private val mediaSession: MediaSessionCompat,
    private var player: MediaPlayer? = null
) : MediaSessionCompat.Callback() {

    private var mediaUri: Uri? = null
    private var newMedia = false
    private var mediaExtras: Bundle? = null
    private var focusRequest: AudioFocusRequest? = null
    private val audioAttributes = AudioAttributes.Builder()
        .run {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            build()
        }


    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        Timber.d("onPlayFromUri: $uri")
        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
    }

    override fun onPlay() {
        super.onPlay()
        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            initializeMediaPlayer()
            prepareMedia()
        }
        Timber.d("onPlay called")
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
        Timber.d("onStop called")
    }

    override fun onPause() {
        super.onPause()
        pausePlaying()
        Timber.d("onPause called")
    }

    private fun setState(state: Int) {
        val position: Long = player?.currentPosition?.toLong() ?: -1L
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

    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }

    private fun ensureAudioFocus(): Boolean {
        val manager = context.getAudioManager()
        if (VersionUtils.isOreo()) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .run {
                    setAudioAttributes(audioAttributes)
                    build()
                }
            this.focusRequest = focusRequest
            val result = manager.requestAudioFocus(focusRequest)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val result = manager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudionFocus() {
        val manager = context.getAudioManager()
        if (VersionUtils.isOreo()) {
            focusRequest?.let {
                manager.abandonAudioFocusRequest(it)
            }
        } else {
            manager.abandonAudioFocus(null)
        }
    }

    private fun initializeMediaPlayer() {
        if (player == null) {
            player = MediaPlayer()
            player?.let { player ->
                player.apply {
                    setOnCompletionListener { setState(PlaybackStateCompat.STATE_PAUSED) }
                    setOnPreparedListener { startPlaying() }
                    setOnBufferingUpdateListener { mediaPlayer, i ->
                        Timber.d("Buffering $mediaPlayer, $i")
                    }
                }
            }
        }
    }

    private fun prepareMedia() {
        if (newMedia) {
            newMedia = false
            player?.let { player ->
                mediaUri?.let { uri ->
                    player.reset()
                    player.setDataSource(context, uri)
                    player.prepareAsync()
                    mediaSession.setMetadata(
                        MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
                            .build()
                    )
                }
            }
        }
    }

    private fun startPlaying() {
        player?.let { player ->
            if (!player.isPlaying) {
                player.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        abandonAudionFocus()
        player?.let { player ->
            if (player.isPlaying) {
                player.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        abandonAudionFocus()
        mediaSession.isActive = false
        player?.let { player ->
            if (player.isPlaying) {
                player.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }
}