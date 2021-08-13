package com.colisa.podplay.repository

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

class PodcastRepo(
    private var feedService: FeedService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun getFeed(feedUrl: String) = flow {
        emit(Result.Loading)
        try {
            val r = feedService.getFeed(feedUrl)
            emit(Result.Success(rssResponseToPodcast(feedUrl, "", r)))
        } catch (e: Exception) {
            val message = e.message
            if (null != message) {
                emit(Result.Error(message))
            } else {
                emit(Result.Error("Unexpected error", e))
            }
        }
    }.flowOn(ioDispatcher)

    private fun rssItemsToEpisodes(rssEpisodes: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return rssEpisodes.map {
            Episode(
                it.guid ?: "",
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""

            )
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String,
        imageUrl: String,
        rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description =
            if (rssResponse.description == "") rssResponse.summary else rssResponse.description
        return Podcast(
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }
}