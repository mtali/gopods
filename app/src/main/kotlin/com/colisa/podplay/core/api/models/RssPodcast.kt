package com.colisa.podplay.core.api.models

import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.models.Episode

data class RssPodcast(
  val url: String,
  val title: String,
  val description: String,
  val lastBuildDate: String,
  val episodes: List<RssEpisode> = emptyList(),
)

data class RssEpisode(
  val guid: String?,
  val title: String?,
  val description: String?,
  val content: String?,
  val audio: String?,
  val pubDate: String?,
  val duration: String?,
  val episodeType: String?,
  val image: String?,
)

/**
 * guid is the primary key, so it falls back to the audio url and then the title.
 * Feeds that omit guid used to collapse every item onto a single empty key.
 */
fun RssPodcast.asEpisodes(podcastId: Long? = null): List<Episode> = episodes
  .map { item ->
    Episode(
      guid = item.guid ?: item.audio ?: item.title.orEmpty(),
      podcastId = podcastId,
      title = item.title.orEmpty(),
      description = item.description ?: item.content.orEmpty(),
      mediaUrl = item.audio.orEmpty(),
      type = item.episodeType.orEmpty(),
      releaseDate = DateUtils.xmlDateToDate(item.pubDate),
      duration = item.duration.orEmpty(),
      imageUrl = item.image.orEmpty(),
    )
  }
