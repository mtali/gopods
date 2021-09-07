package com.colisa.podplay.player

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colisa.podplay.R
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*

private const val NOW_PAYING_CHANNEL_ID = "com.colisa.gopods.NOW_PLAYING"
private const val NOW_PAYING_NOTIFICATION_ID = 0xb339

class NotificationManager(
    private val context: Context,
    private val sessionToken: MediaSessionCompat.Token,
    private val notificationListener: PlayerNotificationManager.NotificationListener
) {
    private var player: Player? = null
    private val job = SupervisorJob()
    private val notifyScope = CoroutineScope(Dispatchers.Main + job)
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOW_PAYING_NOTIFICATION_ID,
            NOW_PAYING_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.np_notification_channel)
            .setChannelDescriptionResourceId(R.string.np_notification_channel_description)
            .setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            .setNotificationListener(notificationListener)
            .build().apply {
                setMediaSessionToken(sessionToken)
                setSmallIcon(R.drawable.ic_episode_play_circle)
                setUseRewindAction(true)
                setUseFastForwardAction(true)
            }
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private var controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null


        override fun getCurrentContentTitle(player: Player): CharSequence =
            player.mediaMetadata.title.toString()

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity


        override fun getCurrentContentText(player: Player): CharSequence =
            player.mediaMetadata.subtitle.toString()


        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = player.mediaMetadata.artworkUri
            return if (currentIconUri != iconUri || currentBitmap == null) {
                // Cache the bitmap for current song
                currentIconUri = iconUri
                notifyScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                Glide.with(context).applyDefaultRequestOptions(glideOptions)
                    .asBitmap()
                    .load(uri)
                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                    .get()
            }
        }

    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 600
private val glideOptions = RequestOptions()
    .fallback(R.drawable.album_art)
    .diskCacheStrategy(DiskCacheStrategy.DATA)