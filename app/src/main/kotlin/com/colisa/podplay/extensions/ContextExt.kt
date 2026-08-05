package com.colisa.podplay.extensions

import android.app.NotificationManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.media.AudioManager

fun Context.notificationManager(): NotificationManager {
    return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
}

fun Context.getAudioManager(): AudioManager {
    return getSystemService(AUDIO_SERVICE) as AudioManager
}