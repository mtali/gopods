package com.colisa.podplay.player

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class GoPlayerService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()

        // Create media session
        createMediaSession()

    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "GoPodsMediaSession")
            .apply {
                setCallback(MediaSessionCallback(this@GoPlayerService, this))
            }
        sessionToken = mediaSession.sessionToken

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

    companion object {
        private const val GO_EMPTY_ROOT_MEDIA_ID = "gopods_empty_root_media"
    }

}