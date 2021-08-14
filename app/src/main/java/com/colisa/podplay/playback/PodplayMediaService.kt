package com.colisa.podplay.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.colisa.podplay.playback.players.MediaSessionCallback

class PodplayMediaService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat

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

    /**
     *  MediaSession that is designed to work with any
     *  media player, either the built-in MediaPlayer or one of your choosing. The
     *  MediaSession provides callbacks for onPlay(), onPause() and onStop() that you’ll use
     *  to create and control the media player.
     */
    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        sessionToken = mediaSession.sessionToken
        val callback = MediaSessionCallback(this, mediaSession)
        mediaSession.setCallback(callback)
    }

    companion object {
        private const val MEDIA_SESSION_TAG = "PodplayMediaService"
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
    }

}