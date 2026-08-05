package com.colisa.podplay.core.common.exts

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE

fun Context.notificationManager(): NotificationManager {
  return getSystemService(NOTIFICATION_SERVICE) as NotificationManager
}
