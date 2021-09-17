package com.colisa.podplay.network.api

import com.colisa.podplay.goRssParser
import com.colisa.podplay.models.Episode
import com.colisa.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class RssPodcast(
    val url: String,
    val title: String,
    val description: String,
    val lastBuildDate: String,
    val episodes: List<RssEpisode> = mutableListOf()
) {
    data class RssEpisode(
        val author: String?,
        val title: String?,
        val content: String?,
        val audio: String?,
        val description: String?,
        val guid: String?,
        val pubDate: String?,
        val image: String?,
        val video: String?,
        val link: String?
    )

    suspend fun toEpisodes(id: Long? = null): List<Episode> {
        return withContext(Dispatchers.Default) {
            episodes.map {
                Episode(
                    guid = it.guid ?: "",
                    podcastId = id,
                    title = it.title ?: "",
                    description = it.description ?: it.content ?: "",
                    mediaUrl = it.audio ?: "",
                    type = "",
                    releaseDate = DateUtils.xmlDateToDate(it.pubDate),
                    duration = ""
                )
            }
        }
    }

}

class RssFeedService : FeedService {


    override suspend fun fetchFeed(feedUrl: String): RssPodcast {
        try {
            val channel = goRssParser.getChannel(feedUrl)
            val podcast = RssPodcast(
                url = feedUrl,
                title = channel.title ?: "",
                description = channel.description ?: "",
                lastBuildDate = channel.lastBuildDate ?: "",
                episodes = channel.articles.map {
                    RssPodcast.RssEpisode(
                        author = it.author,
                        title = it.title,
                        content = it.content,
                        audio = it.audio,
                        description = it.description,
                        guid = it.guid,
                        pubDate = it.pubDate,
                        image = it.image,
                        video = it.video,
                        link = it.link
                    )
                }
            )
            return podcast
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch feed for $feedUrl")
            throw e
        }
    }
}


interface FeedService {

    suspend fun fetchFeed(feedUrl: String): RssPodcast

    companion object {
        val instance: RssFeedService by lazy { RssFeedService() }
    }
}

