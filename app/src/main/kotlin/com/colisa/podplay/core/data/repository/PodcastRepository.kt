package com.colisa.podplay.core.data.repository

import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.models.Podcast
import kotlinx.coroutines.flow.Flow

class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)

interface PodcastRepository {

  fun getPodcasts(subscribed: Boolean): Flow<List<Podcast>>

  fun getPodcastFeed(url: String): Flow<Result<Podcast>>

  suspend fun getPodcast(feedUrl: String): Podcast?

  suspend fun subscribePodcast(podcast: Podcast, subscribed: Boolean)

  suspend fun deletePodcast(podcast: Podcast)

  /** Checks subscribed feeds for episodes that are not stored yet. */
  suspend fun checkNewEpisodes(): List<PodcastUpdateInfo>
}
