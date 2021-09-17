package com.colisa.podplay.network.models

import com.colisa.podplay.models.Podcast
import com.colisa.podplay.util.DateUtils

data class PodcastResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>
) {
    data class ItunesPodcast(
        val collectionId: Long,
        val collectionName: String,
        val feedUrl: String?,
        val artworkUrl100: String,
        val artworkUrl600: String,
        val releaseDate: String
    )

    fun toPodcasts(): List<Podcast> {
        return if (results.isNullOrEmpty()) {
            emptyList()
        } else {
            results
                .filter { it.feedUrl != null }
                .map {
                    Podcast(
                        id = null,
                        collectionId = it.collectionId,
                        feedUrl = it.feedUrl!!,
                        feedTitle = it.collectionName,
                        feedDescription = "",
                        imageUrl = it.artworkUrl100,
                        imageUrl600 = it.artworkUrl600,
                        lastUpdated = DateUtils.parseItunesDate(it.releaseDate)!!,
                        episodes = emptyList()
                    )
                }
        }
    }
}