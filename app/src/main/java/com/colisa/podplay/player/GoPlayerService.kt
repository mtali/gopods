package com.colisa.podplay.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import timber.log.Timber

class GoPlayerService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var notificationManager: NotificationManager

    private val goAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
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

    private var isForeground = false

    private var durationSet: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createMediaSession()

        // Create notification manager
        notificationManager =
            NotificationManager(this, mediaSession.sessionToken, PlayerNotificationListener())

        notificationManager.showNotificationForPlayer(exoPlayer)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.apply {
            stop()
            clearMediaItems()
        }
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
        // Build a PendingIntent that can be used to launch the UI
        val sessionPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { intent ->
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

        // Create a new media session
        mediaSession = MediaSessionCompat(this, "GoPodsMediaSession")
            .apply {
                setSessionActivity(sessionPendingIntent)
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

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            onPlayerStateOrPlayWhenReady(playWhenReady, exoPlayer.playbackState)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            onPlayerStateOrPlayWhenReady(exoPlayer.playWhenReady, playbackState)
            if (playbackState == STATE_READY && !durationSet) {
                mediaSession.setMetadata(
                    MediaMetadataCompat.Builder()
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
                        .build()
                )
                durationSet = true
            }
        }


        private fun onPlayerStateOrPlayWhenReady(playWhenReady: Boolean, state: Int) {
            when (state) {
                STATE_READY,
                Player.STATE_BUFFERING -> {
                    notificationManager.showNotificationForPlayer(exoPlayer)
                    if (state == STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                            isForeground = false
                        }
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }


        override fun onPlayerError(error: PlaybackException) {
            Timber.e("onPlayerError: ${error.message}")
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d("onMediaMetadataChanged: $mediaMetadata")
        }

    }

    private inner class GoPlaybackPrepare : MediaSessionConnector.PlaybackPreparer {
        override fun onCommand(
            player: Player,
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
            val item = extractMediaItem(uri, extras)
            durationSet = false
            exoPlayer.apply {
                setMediaItem(item)
                prepare()
                playWhenReady = whenReady
            }
        }

        private fun extractMediaItem(uri: Uri, extras: Bundle?): MediaItem {
            return if (extras == null) {
                MediaItem.fromUri(uri)
            } else {
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(extras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                            .setSubtitle(extras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                            .setArtworkUri(Uri.parse(extras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
                            .build()
                    )
                    .build()
            }
        }
    }

    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForeground) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@GoPlayerService.javaClass)
                )
                startForeground(notificationId, notification)
                isForeground = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForeground = false
            stopSelf()
        }
    }

    companion object {
        private const val GO_EMPTY_ROOT_MEDIA_ID = "gopods_empty_root_media"
    }

}