package com.colisa.podplay.core.database.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.colisa.podplay.core.models.Episode
import java.util.Date

@Entity(
  tableName = "Episode",
  foreignKeys = [
    ForeignKey(
      entity = PodcastEntity::class,
      parentColumns = ["id"],
      childColumns = ["podcastId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("podcastId")],
)
data class EpisodeEntity(
  @PrimaryKey val guid: String = "",
  val podcastId: Long? = null,
  val title: String = "",
  val description: String = "",
  val mediaUrl: String = "",
  val type: String = "",
  val releaseDate: Date = Date(),
  val duration: String = "",
)

fun EpisodeEntity.asEpisode() = Episode(
  guid = guid,
  podcastId = podcastId,
  title = title,
  description = description,
  mediaUrl = mediaUrl,
  type = type,
  releaseDate = releaseDate,
  duration = duration,
)

fun Episode.asEntity() = EpisodeEntity(
  guid = guid,
  podcastId = podcastId,
  title = title,
  description = description,
  mediaUrl = mediaUrl,
  type = type,
  releaseDate = releaseDate,
  duration = duration,
)
