package com.colisa.podplay.core.api.models

import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.models.Podcast
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class PodcastSearchResponse(
  val resultCount: Int = 0,
  val results: List<ItunesPodcast> = emptyList(),
)

@Serializable
data class ItunesPodcast(
  val collectionId: Long = 0,
  val collectionName: String = "",
  val feedUrl: String? = null,
  val artworkUrl100: String = "",
  val artworkUrl600: String = "",
  val releaseDate: String? = null,
)

/** Results without a feed url cannot be opened, so they are dropped here. */
fun PodcastSearchResponse.asPodcasts(): List<Podcast> = results
  .filter { !it.feedUrl.isNullOrBlank() }
  .map { result ->
    Podcast(
      id = null,
      collectionId = result.collectionId,
      feedUrl = result.feedUrl!!,
      feedTitle = result.collectionName,
      feedDescription = "",
      imageUrl = result.artworkUrl100,
      imageUrl600 = result.artworkUrl600,
      lastUpdated = DateUtils.parseItunesDate(result.releaseDate) ?: Date(),
    )
  }
