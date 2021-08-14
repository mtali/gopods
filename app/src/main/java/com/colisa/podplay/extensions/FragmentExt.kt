package com.colisa.podplay.extensions

import android.support.v4.media.session.MediaControllerCompat
import androidx.fragment.app.Fragment

fun Fragment.requireMediaController(): MediaControllerCompat? {
    return MediaControllerCompat.getMediaController(requireActivity())
}