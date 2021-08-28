package com.colisa.podplay.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colisa.podplay.R
import com.colisa.podplay.db.GoDatabase
import com.colisa.podplay.extensions.notificationManager
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class EpisodeUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val db = GoDatabase.getInstance(applicationContext)
            val repo = PodcastRepo(FeedService.instance, db.podcastDao())
            repo.checkNewSubscribedPodcastsEpisodes().collect { listInfo ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
                for (info in listInfo) {
                    displayNotification(info)
                }
            }
            Result.success()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val manager = applicationContext.notificationManager()
        if (manager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                EPISODE_CHANNEL_ID,
                "Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun displayNotification(info: PodcastRepo.PodcastUpdateInfo) {
        val contentIntent = Intent(applicationContext, MainActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL, info.feedUrl)
        val pendingContentIntent =
            PendingIntent.getActivity(
                applicationContext, 0, contentIntent, FLAG_UPDATE_CURRENT
            )
        val notification = NotificationCompat
            .Builder(applicationContext, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_episode_play_circle)
            .setContentTitle(applicationContext.getString(R.string.episode_notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.episode_notification_text, info.newCount, info.name
                )
            )
            .setNumber(info.newCount)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()

        val manager = applicationContext.notificationManager()
        manager.notify(info.name, 0, notification)
    }

    companion object {
        const val EPISODE_CHANNEL_ID = "gopods_episodes_channel"
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }
}