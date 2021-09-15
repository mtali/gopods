package com.colisa.podplay.models

import androidx.room.Entity

@Entity(primaryKeys = ["term"])
data class PodcastSearchResult(
    val term: String,
    val collectionIds: List<Long>,
    val count: Int
)