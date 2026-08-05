package com.colisa.podplay.core.data.repository.impl

import androidx.room.withTransaction
import com.colisa.podplay.core.api.RssFeedDataSource
import com.colisa.podplay.core.api.models.asEpisodes
import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.data.networkBoundResource
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.data.repository.PodcastUpdateInfo
import com.colisa.podplay.core.data.utils.NetworkMonitor
import com.colisa.podplay.core.database.GoDatabase
import com.colisa.podplay.core.database.daos.PodcastDao
import com.colisa.podplay.core.database.models.asEntity
import com.colisa.podplay.core.database.models.asEpisode
import com.colisa.podplay.core.database.models.asPodcast
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.IO
import com.colisa.podplay.core.models.Episode
import com.colisa.podplay.core.models.Podcast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepositoryImpl @Inject constructor(
  private val rssFeedDataSource: RssFeedDataSource,
  private val podcastDao: PodcastDao,
  private val database: GoDatabase,
  private val networkMonitor: NetworkMonitor,
  @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : PodcastRepository {

  override fun getPodcasts(subscribed: Boolean): Flow<List<Podcast>> =
    podcastDao.getPodcasts(subscribed)
      .map { entities -> entities.map { it.asPodcast() } }
      .flowOn(ioDispatcher)

  override fun getPodcastFeed(url: String): Flow<Result<Podcast>> = networkBoundResource(
    query = {
      podcastDao.loadPodcastByUrl(url)
        .filterNotNull()
        .map { entity ->
          val episodes = entity.id
            ?.let { id -> podcastDao.loadEpisodesOnce(id).map { it.asEpisode() } }
            ?: emptyList()
          entity.asPodcast(episodes)
        }
    },
    fetch = {
      rssFeedDataSource.fetchFeed(url)
    },
    saveFetchResult = { feed ->
      // Only feeds already stored by a search can be refreshed.
      val stored = podcastDao.getPodcast(url)
      if (stored != null) {
        val updated = stored.copy(feedDescription = feed.description)
        database.withTransaction {
          val podcastId = podcastDao.insertPodcast(updated)
          podcastDao.insertEpisodes(feed.asEpisodes(podcastId).map { it.asEntity() })
        }
      }
    },
    shouldFetch = { networkMonitor.isOnline.value },
    onFetchFailed = { Timber.e(it, "Failed to refresh feed $url") },
  ).flowOn(ioDispatcher)

  override suspend fun getPodcast(feedUrl: String): Podcast? = withContext(ioDispatcher) {
    podcastDao.getPodcast(feedUrl)?.asPodcast()
  }

  override suspend fun subscribePodcast(podcast: Podcast, subscribed: Boolean) {
    withContext(ioDispatcher) {
      podcastDao.updatePodcasts(podcast.copy(subscribed = subscribed).asEntity())
    }
  }

  /** Episodes are removed by the foreign key cascade. */
  override suspend fun deletePodcast(podcast: Podcast) {
    withContext(ioDispatcher) {
      podcastDao.deletePodcast(podcast.asEntity())
    }
  }

  override suspend fun checkNewEpisodes(): List<PodcastUpdateInfo> =
    withContext(ioDispatcher) {
      val subscribed = podcastDao.loadSubscribedPodcasts(subscribed = true)
      if (subscribed.isEmpty()) {
        Timber.d("No subscribed podcasts")
        return@withContext emptyList()
      }

      subscribed.mapNotNull { entity ->
        val podcastId = entity.id ?: return@mapNotNull null
        val newEpisodes = findNewEpisodes(entity.feedUrl, podcastId)
        if (newEpisodes.isEmpty()) {
          Timber.d("No new episodes for: ${entity.feedTitle}")
          return@mapNotNull null
        }
        podcastDao.insertEpisodes(newEpisodes.map { it.asEntity() })
        PodcastUpdateInfo(entity.feedUrl, entity.feedTitle, newEpisodes.size)
      }
    }

  /** Returns an empty list on failure so one broken feed does not stop the rest. */
  private suspend fun findNewEpisodes(feedUrl: String, podcastId: Long): List<Episode> {
    return try {
      val remote = rssFeedDataSource.fetchFeed(feedUrl).asEpisodes(podcastId)
      val storedGuids = podcastDao.loadEpisodesOnce(podcastId).map { it.guid }.toSet()
      remote.filterNot { it.guid in storedGuids }
    } catch (e: Throwable) {
      Timber.e(e, "Failed to check episodes for $feedUrl")
      emptyList()
    }
  }
}
