package com.colisa.podplay.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.colisa.podplay.app.goPreferences

/**
 * Media3 owns the notification, lock screen controls, audio focus, media buttons and
 * the foreground service lifecycle, so none of that is handled here.
 */
class PlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()

    val seekStepMs = goPreferences.fastSeekingStep * 1000L

    val player = ExoPlayer.Builder(this)
      .setAudioAttributes(audioAttributes, true)
      .setHandleAudioBecomingNoisy(true)
      .setSeekBackIncrementMs(seekStepMs)
      .setSeekForwardIncrementMs(seekStepMs)
      .build()

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(launchAppIntent())
      .build()
  }

  private fun launchAppIntent(): PendingIntent {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

  /**
   * Playback survives swiping the app away; the notification stays as the control
   * surface. The service only stops when nothing is loaded or paused.
   */
  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
    }
    mediaSession = null
    super.onDestroy()
  }
}
