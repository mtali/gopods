package com.colisa.podplay.data

import com.colisa.podplay.db.GoDatabase
import com.colisa.podplay.goRssParser
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.networkBoundResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber


class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)

class PodcastRepo(
    private var feedService: FeedService,
    private var db: GoDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val podcastDao = db.podcastDao()

    /**
     * Retrieve podcast from database and in parallel start a background task
     * to fetch fresh podcast and episodes
     */
    suspend fun getPodcastFeed(url: String) = networkBoundResource(
        query = {
            podcastDao.loadPodcastByUrl(url).mapLatest { podcast ->
                podcast.episodes = podcastDao.loadPodcastEpisodesStatic(podcast.id!!)
                podcast
            }
        },

        fetch = {
            feedService.fetchFeed(url)
        },

        saveFetchResult = { response ->
            val newPodcast = podcastDao.getPodcast(url)!!.copy(
                feedDescription = response.description,
                episodes = response.toEpisodes()
            )
            db.runInTransaction {
                val id = podcastDao.insertPodcast(newPodcast)
                for (episode in newPodcast.episodes) {
                    episode.podcastId = id
                    podcastDao.insertEpisode(episode)
                }
            }
        },

        shouldFetch = { true },

        onFetchFailed = {
            Timber.e(it, "onFetchFailed: failed to fetch or save response")
        }

    ).flowOn(ioDispatcher)


    /**
     * Update subscription status of a podcast
     */
    suspend fun subscribePodcast(podcast: Podcast, subscribed: Boolean) {
        podcast.apply {
            this.subscribed = subscribed
        }
        podcastDao.updatePodcasts(podcast)
    }

    /**
     * Will delete related episodes i.e FK constraints
     */
    suspend fun deletePodcast(podcast: Podcast) = withContext(ioDispatcher) {
        podcastDao.deletePodcast(podcast)
    }

    /**
     * Check online and local podcasts and determine new episodes
     * On error return empty list
     */
    private suspend fun getNewEpisodes(podcast: Podcast): List<Episode> {
        return try {
            val newEpisodes = podcast.id?.let { id ->
                // Clear cache - make sure fresh data not cached
                goRssParser.flushCache(podcast.feedUrl)
                val rs = feedService.fetchFeed(podcast.feedUrl)
                val remote = rs.toEpisodes(id)
                val local = podcastDao.loadEpisodes(id).first()
                val new = remote.filter { episode ->
                    local.find { episode.guid == it.guid } == null
                }
                return@let new
            }
            newEpisodes ?: emptyList()
        } catch (e: Throwable) {
            emptyList()
        }
    }

    /**
     * Initiate checking for new episodes only for podcasts that user have subscribed
     * Used for PeriodicWork
     */
    suspend fun checkNewEpisodes(): List<PodcastUpdateInfo>? = withContext(ioDispatcher) {
        val podcasts = podcastDao.loadSubscribedPodcastsStatic(subscribed = true)
        if (!podcasts.isNullOrEmpty()) {
            val info = mutableListOf<PodcastUpdateInfo>()
            var count = podcasts.count()
            for (podcast in podcasts) {
                val newEpisodes = getNewEpisodes(podcast)
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    info.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl, podcast.feedTitle, newEpisodes.count()
                        )
                    )
                } else {
                    Timber.d("No new episodes for: ${podcast.feedTitle}")
                }
                count--
                if (count == 0) {
                    return@withContext info
                }

            }
        } else {
            Timber.d("No subscribed podcast")
        }
        return@withContext null
    }


    /**
     * Save episodes for a particular podcast
     */
    private suspend fun saveNewEpisodes(podcastId: Long, episode: List<Episode>) {
        withContext(ioDispatcher) {
            for (e in episode) {
                e.podcastId = podcastId
                podcastDao.insertEpisode(e)
            }
        }
    }

    /**
     * Retrieve flow of podcasts flag: subscribed or not
     */
    fun getPodcasts(subscribed: Boolean) = podcastDao.getPodcasts(subscribed)

}