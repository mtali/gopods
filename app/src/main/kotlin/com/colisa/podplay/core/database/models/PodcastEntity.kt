package com.colisa.podplay.core.database.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.colisa.podplay.core.models.Episode
import com.colisa.podplay.core.models.Podcast
import java.util.Date

/**
 * Table and column names are pinned to what shipped in version 1 of the database so
 * existing installs keep their subscriptions.
 */
@Entity(
  tableName = "Podcast",
  indices = [Index(value = ["collectionId"], unique = true)],
)
data class PodcastEntity(
  @PrimaryKey(autoGenerate = true) val id: Long? = null,
  val collectionId: Long = 0,
  val feedUrl: String = "",
  val feedTitle: String = "",
  val feedDescription: String = "",
  val imageUrl: String = "",
  val imageUrl600: String = "",
  val lastUpdated: Date = Date(),
  val subscribed: Boolean = false,
)

fun PodcastEntity.asPodcast(episodes: List<Episode> = emptyList()) =
  Podcast(
    id = id,
    collectionId = collectionId,
    feedUrl = feedUrl,
    feedTitle = feedTitle,
    feedDescription = feedDescription,
    imageUrl = imageUrl,
    imageUrl600 = imageUrl600,
    lastUpdated = lastUpdated,
    subscribed = subscribed,
    episodes = episodes,
  )

fun Podcast.asEntity() = PodcastEntity(
  id = id,
  collectionId = collectionId,
  feedUrl = feedUrl,
  feedTitle = feedTitle,
  feedDescription = feedDescription,
  imageUrl = imageUrl,
  imageUrl600 = imageUrl600,
  lastUpdated = lastUpdated,
  subscribed = subscribed,
)
