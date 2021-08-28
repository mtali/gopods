package com.colisa.podplay.extensions

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE

fun Context.notificationManager(): NotificationManager {
    return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
}