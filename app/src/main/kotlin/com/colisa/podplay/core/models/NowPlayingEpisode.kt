package com.colisa.podplay.core.models

import com.squareup.moshi.JsonClass

/**
 * Metadata for the episode loaded in the player. Persisted so the mini player can
 * render before the media controller has connected.
 */
@JsonClass(generateAdapter = true)
data class NowPlayingEpisode(
  var title: String = "",
  var artUrl: String = "",
  var artUrl600: String = "",
  var mediaUrl: String = "",
  var description: String = "",
  var podcastTitle: String = "",
)
