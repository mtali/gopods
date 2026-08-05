package com.colisa.podplay.ui

import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.colisa.podplay.core.models.NowPlayingEpisode
import com.colisa.podplay.core.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Exposes player state as LiveData because the layouts still bind to it directly.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
  playerConnection: PlayerConnection,
) : ViewModel() {

  private val state = playerConnection.state

  val recentEpisode: LiveData<NowPlayingEpisode?> =
    state.map { it.episode }.asLiveData()

  val isPlaying: LiveData<Boolean> =
    state.map { it.isPlaying }.asLiveData()

  val episodeDuration: LiveData<Long> =
    state.map { it.durationMs }.asLiveData()

  val formattedDuration: LiveData<String> =
    state.map { DateUtils.formatElapsedTime(it.durationMs / 1000) }.asLiveData()

  val formattedCurrentTime: LiveData<String> =
    state.map { DateUtils.formatElapsedTime(it.positionMs / 1000) }.asLiveData()

  val positionMs: LiveData<Long> =
    state.map { it.positionMs }.asLiveData()

  val podcastTitleOrBuffering: LiveData<String> = state.map { player ->
    if (player.isBuffering) {
      "Buffering ..."
    } else {
      player.episode?.podcastTitle ?: "Loading ..."
    }
  }.asLiveData()
}
