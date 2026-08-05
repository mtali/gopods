package com.colisa.podplay.core.models

import java.util.Date

data class Episode(
  val guid: String = "",
  val podcastId: Long? = null,
  val title: String = "",
  val description: String = "",
  val mediaUrl: String = "",
  val type: String = "",
  val releaseDate: Date = Date(),
  val duration: String = "",
  /** Per episode artwork when the feed provides it; empty falls back to the podcast. */
  val imageUrl: String = "",
)
