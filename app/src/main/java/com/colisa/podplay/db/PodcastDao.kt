package com.colisa.podplay.db

import android.util.SparseLongArray
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.models.PodcastSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast ORDER BY feedTitle")
    fun getAllPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM podcast WHERE subscribed = :subscribed ORDER BY feedTitle")
    fun getPodcasts(subscribed: Boolean = true): Flow<List<Podcast>>

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


    /**
     * Insert search result to database
     * Code below is responsible for caching search results
     */
    @Insert(onConflict = REPLACE)
    fun insertSearchResult(result: PodcastSearchResult)

    @Insert(onConflict = REPLACE)
    fun insertPodcasts(podcasts: List<Podcast>)

    @Query("SELECT * FROM Podcast WHERE collectionId in (:collectIds)")
    fun loadPodcastsById(collectIds: List<Long>): Flow<List<Podcast>>

    @Query("SELECT * FROM PodcastSearchResult WHERE term = :term")
    suspend fun loadSearchResult(term: String): PodcastSearchResult?

    fun loadPodcastOrdered(ids: List<Long>): Flow<List<Podcast>> {
        val order = SparseLongArray()
        ids.withIndex().forEach {
            order.put(it.value.toInt(), it.index.toLong())
        }
        return loadPodcastsById(ids).map { podcasts ->
            podcasts.sortedWith(compareBy { order.get(it.collectionId.toInt()) })
        }
    }
}