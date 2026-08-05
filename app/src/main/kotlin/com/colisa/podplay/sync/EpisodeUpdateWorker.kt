package com.colisa.podplay.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colisa.podplay.R
import com.colisa.podplay.app.goPreferences
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.data.repository.PodcastUpdateInfo
import com.colisa.podplay.extensions.notificationManager
import com.colisa.podplay.ui.MainActivity
import com.colisa.podplay.util.Utils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class EpisodeUpdateWorker @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted params: WorkerParameters,
  private val podcastRepository: PodcastRepository,
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    val updates = podcastRepository.checkNewEpisodes()
    if (!goPreferences.notifyEpisodeUpdates) {
      Timber.d("Episode update notification disabled")
      return Result.success()
    }
    if (updates.isNotEmpty()) {
      createNotificationChannel()
      updates.forEach { displayNotification(it) }
    }
    return Result.success()
  }

  private fun createNotificationChannel() {
    if (!Utils.isOreo()) return
    val manager = applicationContext.notificationManager()
    if (manager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          EPISODE_CHANNEL_ID,
          applicationContext.getString(R.string.episode_notification_title),
          NotificationManager.IMPORTANCE_DEFAULT,
        ),
      )
    }
  }

  private fun displayNotification(info: PodcastUpdateInfo) {
    val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
      putExtra(EXTRA_FEED_URL, info.feedUrl)
    }
    val pendingContentIntent = PendingIntent.getActivity(
      applicationContext,
      info.feedUrl.hashCode(),
      contentIntent,
      FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat
      .Builder(applicationContext, EPISODE_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_episode_play_circle)
      .setContentTitle(applicationContext.getString(R.string.episode_notification_title))
      .setContentText(
        applicationContext.getString(
          R.string.episode_notification_text, info.newCount, info.name,
        ),
      )
      .setNumber(info.newCount)
      .setAutoCancel(true)
      .setContentIntent(pendingContentIntent)
      .build()

    applicationContext.notificationManager().notify(info.name, 0, notification)
  }

  companion object {
    const val EPISODE_CHANNEL_ID = "gopods_episodes_channel"
    const val EXTRA_FEED_URL = "PodcastFeedUrl"
  }
}
