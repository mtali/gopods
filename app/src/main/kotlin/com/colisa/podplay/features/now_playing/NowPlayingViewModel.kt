package com.colisa.podplay.features.now_playing

import androidx.lifecycle.ViewModel
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.core.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The speeds podcast apps normally offer. */
val PlaybackSpeeds = listOf(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
  private val playerConnection: PlayerConnection,
) : ViewModel() {

  val uiState: StateFlow<PlayerUiState> = playerConnection.state

  fun onPlayPause() = playerConnection.togglePlayPause()

  fun onSeekBack() = playerConnection.seekBy(forward = false)

  fun onSeekForward() = playerConnection.seekBy(forward = true)

  fun onSeekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

  fun onCycleSpeed() {
    val current = uiState.value.speed
    val next = PlaybackSpeeds.firstOrNull { it > current + 0.01f } ?: PlaybackSpeeds.first()
    playerConnection.setSpeed(next)
  }
}
