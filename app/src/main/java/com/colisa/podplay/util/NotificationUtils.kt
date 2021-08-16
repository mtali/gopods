package com.colisa.podplay.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.annotation.RequiresApi

object NotificationUtils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(
        context: Context,
        id: String,
        name: String,
        importance: Int = IMPORTANCE_LOW
    ) {
        if (!Utils.isOreo()) return
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(id, name, IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }
}