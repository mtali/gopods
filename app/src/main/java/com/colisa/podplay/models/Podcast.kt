package com.colisa.podplay.models

import java.util.*

data class Podcast(
    var feedUrl: String = "",
    var feedTitle: String = "",
    var feedDescription: String = "",
    var imageUrl: String = "",
    var lastUpdated: Date = Date(),
    var episodes: List<Episode> = listOf()
)