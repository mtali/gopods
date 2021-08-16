package com.colisa.podplay.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.colisa.podplay.R
import com.colisa.podplay.playback.players.MediaSessionCallback
import com.colisa.podplay.ui.activities.MainActivity
import com.colisa.podplay.util.NotificationUtils
import com.colisa.podplay.util.Utils
import kotlinx.coroutines.*
import java.net.URL

class PodplayMediaService : MediaBrowserServiceCompat(),
    MediaSessionCallback.PodplayMediaSessionListener {
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(PODPLAY_EMPTY_ROOT_MEDIA_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId.equals(PODPLAY_EMPTY_ROOT_MEDIA_ID)) {
            result.sendResult(null)
        }
    }


    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        sessionToken = mediaSession.sessionToken
        val callback = MediaSessionCallback(this, mediaSession, listener = this)
        mediaSession.setCallback(callback)
    }

    private fun getPausePlayActions()
            : Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_outline_pause_24,
            getString(R.string.pause),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )

        val playAction = NotificationCompat.Action(
            R.drawable.ic_outline_play_arrow_24,
            getString(R.string.play),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PLAY
            )
        )
        return Pair(pauseAction, playAction)
    }

    private fun isPlaying(): Boolean {
        return if (mediaSession.controller.playbackState != null) {
            mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING
        } else {
            false
        }
    }

    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent = Intent(this, MainActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this@PodplayMediaService,
            0,
            openActivityIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        NotificationUtils.createNotificationChannel(this, PLAYER_CHANNEL_ID, "Player")
    }

    private fun createNotification(mediaDescription: MediaDescriptionCompat, bitmat: Bitmap?)
            : Notification {
        val intent = getNotificationIntent()
        val (pause, play) = getPausePlayActions()
        val notification = NotificationCompat.Builder(this, PLAYER_CHANNEL_ID)
        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmat)
            .setContentIntent(intent)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(if (isPlaying()) pause else play)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )

            .build()
        return notification.build()
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    private fun displayNotification() {
        if (mediaSession.controller.metadata == null) return
        if (Utils.isOreo()) createNotificationChannel()
        val mediaDescription = mediaSession.controller.metadata.description
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val iconUrl = URL(mediaDescription.iconUri.toString())
                val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())
                val notification = createNotification(mediaDescription, bitmap)
                ContextCompat.startForegroundService(
                    this@PodplayMediaService,
                    Intent(this@PodplayMediaService, PodplayMediaService::class.java)
                )
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onStateChanged() {
        displayNotification()
    }

    override fun onPausePlaying() {
        stopForeground(false)
    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }


    companion object {
        private const val MEDIA_SESSION_TAG = "PodplayMediaService"
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
        private const val PLAYER_CHANNEL_ID = "podplay_player_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

}