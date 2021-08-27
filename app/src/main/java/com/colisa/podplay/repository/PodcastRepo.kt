package com.colisa.podplay.repository

import androidx.lifecycle.LiveData
import com.colisa.podplay.db.PodcastDao
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Result
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.models.RssFeedResponse
import com.colisa.podplay.util.DateUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun getLivePodcastFeed(feedUrl: String) = flow {
        emit(Result.Loading)
        try {
            val r = feedService.getFeed(feedUrl)
            emit(Result.OK(rssResponseToPodcast(feedUrl, "", r)))
        } catch (e: Throwable) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)


    suspend fun savePodcast(podcast: Podcast) = withContext(ioDispatcher) {
        val id = podcastDao.insertPodcast(podcast)
        for (episode in podcast.episodes) {
            episode.podcastId = id
            podcastDao.insertEpisode(episode)
        }
    }


    fun getSubscribedPodcasts(): LiveData<List<Podcast>> = podcastDao.getPodcasts()

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
        rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description =
            if (rssResponse.description == "") rssResponse.summary else rssResponse.description
        return Podcast(
            null,
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }
}