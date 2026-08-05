package com.colisa.podplay.core.models

import kotlinx.serialization.Serializable

/**
 * Metadata for the episode loaded in the player. Persisted so the mini player can
 * render before the media controller has connected.
 *
 * Field names match what the previous Moshi based storage wrote, so an episode saved
 * by an older install still reads back.
 */
@Serializable
data class NowPlayingEpisode(
  val title: String = "",
  val artUrl: String = "",
  val artUrl600: String = "",
  val mediaUrl: String = "",
  val description: String = "",
  val podcastTitle: String = "",
)
