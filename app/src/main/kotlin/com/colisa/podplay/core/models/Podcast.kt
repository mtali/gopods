package com.colisa.podplay.core.models

import java.util.Date

data class Podcast(
  val id: Long? = null,
  val collectionId: Long = 0,
  val feedUrl: String = "",
  val feedTitle: String = "",
  val feedDescription: String = "",
  val imageUrl: String = "",
  val imageUrl600: String = "",
  val lastUpdated: Date = Date(),
  val subscribed: Boolean = false,
  val episodes: List<Episode> = emptyList(),
)
