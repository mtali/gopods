package com.colisa.podplay.features.now_playing

import androidx.lifecycle.ViewModel
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.core.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
  private val playerConnection: PlayerConnection,
) : ViewModel() {

  val uiState: StateFlow<PlayerUiState> = playerConnection.state

  fun onPlayPause() = playerConnection.togglePlayPause()

  fun onSeekBack() = playerConnection.seekBy(forward = false)

  fun onSeekForward() = playerConnection.seekBy(forward = true)

  fun onSeekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
}
