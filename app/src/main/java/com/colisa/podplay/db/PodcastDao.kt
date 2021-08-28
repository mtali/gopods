package com.colisa.podplay.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast ORDER BY feedTitle")
    fun getPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    fun getEpisodes(podcastId: Long): List<Episode>

    @Query("SELECT * FROM podcast WHERE feedUrl = :url")
    fun getPodcast(url: String): Podcast?

    @Delete
    fun deletePodcast(podcast: Podcast)

    @Insert(onConflict = REPLACE)
    fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    fun insertEpisode(episode: Episode): Long

    @Query("SELECT * FROM podcast ORDER BY feedTitle")
    suspend fun getPodcastsStatic(): List<Podcast>
}