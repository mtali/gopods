package com.colisa.podplay.core.database.models

import androidx.room.Entity

@Entity(tableName = "PodcastSearchResult", primaryKeys = ["term"])
data class PodcastSearchResultEntity(
  val term: String,
  val collectionIds: List<Long>,
  val count: Int,
)
