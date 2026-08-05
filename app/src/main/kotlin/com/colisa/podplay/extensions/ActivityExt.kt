package com.colisa.podplay.extensions

import android.app.Activity
import android.support.v4.media.session.MediaControllerCompat

fun Activity.onMediaController(block: (controller: MediaControllerCompat) -> Unit) {
    val controller = MediaControllerCompat.getMediaController(this)
    controller?.let {
        block(it)
    }
}