package com.colisa.podplay.player

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import timber.log.Timber

class GoPlayerService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    private val goAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()

    // ExoPlayer will handle audio focus for us
    private val exoPlayer: ExoPlayer
            by lazy {
                SimpleExoPlayer.Builder(this).build().apply {
                    setAudioAttributes(goAudioAttributes, true)
                    setHandleAudioBecomingNoisy(true)
                    addListener(playerListener)
                }
            }

    // Media connection
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.run {
            isActive = false
            release()
        }

        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    private fun createMediaSession() {
        // Create a new media session
        mediaSession = MediaSessionCompat(this, "GoPodsMediaSession")
            .apply {
                isActive = true
            }

        // Set token
        sessionToken = mediaSession.sessionToken

        // ExoPlayer will menage media session for us
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(GoPlaybackPrepare())
        mediaSessionConnector.setPlayer(exoPlayer)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(GO_EMPTY_ROOT_MEDIA_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == GO_EMPTY_ROOT_MEDIA_ID) {
            result.sendResult(null)
        }
    }

    /**
     * Listen events from exoplayer
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_IDLE -> {
                    Timber.d("Player idle")
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("Player buffering")
                }
                Player.STATE_ENDED -> {
                    Timber.d("Player ended")
                }
                Player.STATE_READY -> {
                    Timber.d("Player ready")
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.d("onPlayerError: ${error.message}")
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: ${mediaMetadata.mediaUri}")
        }
    }

    private inner class GoPlaybackPrepare : MediaSessionConnector.PlaybackPreparer {
        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean = false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY_FROM_URI

        override fun onPrepare(playWhenReady: Boolean) = Unit

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) = Unit

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) =
            Unit

        override fun onPrepareFromUri(uri: Uri, whenReady: Boolean, extras: Bundle?) {
            val item = MediaItem.fromUri(uri)
            exoPlayer.apply {
                setMediaItem(item)
                prepare()
                playWhenReady = whenReady
            }
        }
    }

    companion object {
        private const val GO_EMPTY_ROOT_MEDIA_ID = "gopods_empty_root_media"
    }

}