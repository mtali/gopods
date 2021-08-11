package com.colisa.podplay.network.models

data class PodcastResponse(
    val resultCount: Int,
    val results: List<ItunesPodcast>
) {
    data class ItunesPodcast(
        val collectionName: String,
        val feedUrl: String,
        val artworkUrl100: String,
        val releaseDate: String
    )
}