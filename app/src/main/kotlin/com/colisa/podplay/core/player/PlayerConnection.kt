package com.colisa.podplay.core.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.colisa.podplay.app.goPreferences
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.Main
import com.colisa.podplay.core.models.NowPlayingEpisode
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val POSITION_TICK_MS = 500L

/**
 * Single point of contact with the playback service. The UI observes [state] and never
 * touches the media session directly.
 *
 * A MediaController may only be used from the main thread, so everything here runs on
 * the main dispatcher.
 */
@Singleton
class PlayerConnection @Inject constructor(
  @param:ApplicationContext private val context: Context,
  @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
) {

  private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

  private var controller: MediaController? = null
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var positionJob: Job? = null

  /** Set when play is requested before the controller has finished connecting. */
  private var pendingEpisode: NowPlayingEpisode? = null

  private val _state = MutableStateFlow(PlayerUiState(episode = goPreferences.latestEpisode))
  val state: StateFlow<PlayerUiState> = _state.asStateFlow()

  private val _errors = MutableSharedFlow<String>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  val errors: SharedFlow<String> = _errors.asSharedFlow()

  private val listener = object : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
      publish(player)
    }

    override fun onPlayerError(error: PlaybackException) {
      Timber.e(error, "Playback error")
      _errors.tryEmit(error.localizedMessage ?: "Error playing episode")
    }
  }

  fun connect() {
    if (controller != null || controllerFuture != null) return

    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        val connected = runCatching { future.get() }
          .onFailure { Timber.e(it, "Failed to connect to playback service") }
          .getOrNull() ?: return@addListener

        controller = connected
        connected.addListener(listener)
        publish(connected)
        pendingEpisode?.let { episode ->
          pendingEpisode = null
          play(episode)
        }
      },
      ContextCompat.getMainExecutor(context),
    )
  }

  fun release() {
    stopTicker()
    controller?.removeListener(listener)
    controller = null
    controllerFuture?.let { MediaController.releaseFuture(it) }
    controllerFuture = null
  }

  fun play(episode: NowPlayingEpisode) {
    goPreferences.latestEpisode = episode
    _state.update { it.copy(episode = episode) }

    val active = controller
    if (active == null) {
      pendingEpisode = episode
      connect()
      return
    }

    // Resume instead of restarting when the same episode is already loaded.
    if (active.currentMediaItem?.mediaId == episode.mediaUrl) {
      active.play()
      return
    }

    active.setMediaItem(episode.asMediaItem())
    active.prepare()
    active.play()
  }

  fun togglePlayPause() {
    val active = controller ?: return
    if (active.isPlaying) {
      active.pause()
      return
    }
    if (active.currentMediaItem == null) {
      goPreferences.latestEpisode?.let { play(it) }
      return
    }
    active.play()
  }

  /**
   * Seeks by the step configured in settings. The value is read here rather than baked
   * into the player so a change in settings takes effect immediately.
   */
  fun seekBy(forward: Boolean) {
    val active = controller ?: return
    val step = goPreferences.fastSeekingStep * 1000L
    val target = active.currentPosition + if (forward) step else -step
    seekTo(target)
  }

  fun seekTo(positionMs: Long) {
    val active = controller ?: return
    val duration = active.duration
    val upperBound = if (duration == C.TIME_UNSET) Long.MAX_VALUE else duration
    active.seekTo(positionMs.coerceIn(0, upperBound))
    publish(active)
  }

  private fun publish(player: Player) {
    val duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0L
    _state.update {
      it.copy(
        isPlaying = player.isPlaying,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        positionMs = player.currentPosition.coerceAtLeast(0),
        durationMs = duration,
      )
    }
    if (player.isPlaying) startTicker() else stopTicker()
  }

  private fun startTicker() {
    if (positionJob?.isActive == true) return
    positionJob = scope.launch {
      while (isActive) {
        controller?.let { active ->
          _state.update { it.copy(positionMs = active.currentPosition.coerceAtLeast(0)) }
        }
        delay(POSITION_TICK_MS)
      }
    }
  }

  private fun stopTicker() {
    positionJob?.cancel()
    positionJob = null
  }
}

private fun NowPlayingEpisode.asMediaItem(): MediaItem = MediaItem.Builder()
  .setMediaId(mediaUrl)
  .setUri(mediaUrl)
  .setMediaMetadata(
    MediaMetadata.Builder()
      .setTitle(title)
      .setArtist(podcastTitle)
      .setArtworkUri(artUrl600.takeIf { it.isNotBlank() }?.toUri())
      .setIsBrowsable(false)
      .setIsPlayable(true)
      .build(),
  )
  .build()
