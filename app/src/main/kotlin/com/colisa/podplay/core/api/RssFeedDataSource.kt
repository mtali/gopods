package com.colisa.podplay.core.api

import com.colisa.podplay.core.api.models.RssEpisode
import com.colisa.podplay.core.api.models.RssPodcast
import com.prof18.rssparser.RssParserBuilder
import okhttp3.Call
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFeedDataSource @Inject constructor(callFactory: Call.Factory) {

  /**
   * Built here rather than provided by Hilt: rssparser is compiled with a newer Kotlin
   * than Hilt 2.58 can read metadata for, so RssParser must stay out of the DI graph.
   *
   * No charset is forced. The previous setup pinned ISO-8859-7, which mangled
   * non-ASCII text in most feeds; the parser now detects the encoding itself.
   */
  private val rssParser = RssParserBuilder(callFactory = callFactory).build()

  suspend fun fetchFeed(feedUrl: String): RssPodcast {
    try {
      val channel = rssParser.getRssChannel(feedUrl)
      return RssPodcast(
        url = feedUrl,
        title = channel.title.orEmpty(),
        description = channel.description.orEmpty(),
        lastBuildDate = channel.lastBuildDate.orEmpty(),
        episodes = channel.items.map { item ->
          RssEpisode(
            guid = item.guid,
            title = item.title,
            description = item.description,
            content = item.content,
            audio = item.audio,
            pubDate = item.pubDate,
            duration = item.itunesItemData?.duration,
            episodeType = item.itunesItemData?.episodeType,
          )
        },
      )
    } catch (e: Exception) {
      Timber.e(e, "Failed to fetch feed for $feedUrl")
      throw e
    }
  }
}
