package com.colisa.podplay.repository

import com.colisa.podplay.db.PodcastDao
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Result
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.models.RssFeedResponse
import com.colisa.podplay.util.DateUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber

class PodcastRepo(
    private var feedService: FeedService,
    private var podcastDao: PodcastDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {


    fun getPodcasts(feedUrl: String) = flow {
        emit(Result.Loading)
        val podcast = podcastDao.getPodcast(feedUrl)
        if (podcast != null) {
            podcast.id?.let {
                podcast.episodes = podcastDao.getEpisodes(it)
            }
            emit(Result.OK(podcast))
        } else {
            try {
                val r = feedService.getFeed(feedUrl)
                emit(Result.OK(rssResponseToPodcast(feedUrl, "", "", r)))
            } catch (e: Throwable) {
                emit(Result.Error(e))
            }
        }

    }.flowOn(ioDispatcher)


    suspend fun savePodcast(podcast: Podcast) = withContext(ioDispatcher) {
        val id = podcastDao.insertPodcast(podcast)
        for (episode in podcast.episodes) {
            episode.podcastId = id
            podcastDao.insertEpisode(episode)
        }
    }

    suspend fun deletePodcast(podcast: Podcast) = withContext(ioDispatcher) {
        podcastDao.deletePodcast(podcast)
    }


    private fun getNewEpisodes(localPodcast: Podcast) = flow {
        try {
            val res = feedService.getFeed(localPodcast.feedUrl)
            val remotePodcast =
                rssResponseToPodcast(
                    localPodcast.feedUrl,
                    localPodcast.imageUrl,
                    localPodcast.imageUrl600,
                    res
                )
            remotePodcast?.let {
                val localEpisode = podcastDao.getEpisodes(localPodcast.id!!)
                val newEpisodes = remotePodcast.episodes.filter { episode ->
                    localEpisode.find { episode.guid == it.guid } == null
                }
                emit(Result.OK(newEpisodes))
            }
        } catch (e: Throwable) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)

    private suspend fun saveNewEpisodes(podcastId: Long, episode: List<Episode>) {
        withContext(ioDispatcher) {
            for (e in episode) {
                e.podcastId = podcastId
                podcastDao.insertEpisode(e)
            }
        }
    }

    fun checkNewSubscribedPodcastsEpisodes() = flow {
        val updateInfo = mutableListOf<PodcastUpdateInfo>()
        val podcasts = podcastDao.getPodcastsStatic()
        var processCount = podcasts.count()
        for (podcast in podcasts) {
            getNewEpisodes(podcast)
                .collect {
                    if (it is Result.OK) {
                        val newEpisodes = it.data
                        if (newEpisodes.count() > 0) {
                            saveNewEpisodes(podcast.id!!, newEpisodes)
                            updateInfo.add(
                                PodcastUpdateInfo(
                                    podcast.feedUrl, podcast.feedTitle, newEpisodes.count()
                                )
                            )
                        }
                        processCount--
                        if (processCount == 0) {
                            emit(updateInfo)
                        }
                    }
                }
        }
    }.flowOn(ioDispatcher)

//    fun getSubscribedPodcasts(): LiveData<List<Podcast>> = podcastDao.getPodcasts()

    fun getPodcasts(subscribed: Boolean) = podcastDao.getPodcasts(subscribed)

    private fun rssItemsToEpisodes(rssEpisodes: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        Timber.d("rssItemsToEpisodes() Thread: ${Thread.currentThread().name}")
        return rssEpisodes.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""

            )
        }
    }

    @Suppress("SameParameterValue")
    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String,
        imageUrl600: String,
        rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description =
            if (rssResponse.description == "") rssResponse.summary else rssResponse.description
        return Podcast(
            id = null,
            feedUrl = feedUrl,
            feedTitle = rssResponse.title,
            feedDescription = description,
            imageUrl = imageUrl,
            imageUrl600 = imageUrl600,
            lastUpdated = rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }

    /**
     * Hold update details for a single podcast
     */
    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)
}