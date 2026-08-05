package com.colisa.podplay.core.player

import com.colisa.podplay.core.models.NowPlayingEpisode

data class PlayerUiState(
  val episode: NowPlayingEpisode? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0,
  val durationMs: Long = 0,
)
