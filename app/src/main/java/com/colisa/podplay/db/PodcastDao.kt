package com.colisa.podplay.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast ORDER BY feedTitle")
    fun getPodcasts(): Flow<List<Podcast>>

    @Query("SELECT * FROM episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    fun getEpisodes(podcastId: Long): List<Episode>

    @Insert(onConflict = REPLACE)
    fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    fun insertEpisode(episode: Episode): Long
}