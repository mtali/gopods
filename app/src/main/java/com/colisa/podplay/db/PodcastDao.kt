package com.colisa.podplay.db

import android.util.SparseLongArray
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.models.PodcastSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface PodcastDao {

    @Query("SELECT * FROM podcast WHERE subscribed = :subscribed ORDER BY feedTitle")
    fun getPodcasts(subscribed: Boolean = true): Flow<List<Podcast>>

    @Query("SELECT * FROM podcast WHERE feedUrl = :url")
    fun getPodcast(url: String): Podcast?

    @Delete
    fun deletePodcast(podcast: Podcast)

    @Insert(onConflict = REPLACE)
    fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    fun insertEpisode(episode: Episode): Long


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

    @Query("SELECT * FROM episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    fun loadEpisodes(podcastId: Long): Flow<List<Episode>>

    @Query("SELECT * FROM podcast WHERE feedUrl = :url")
    fun loadPodcastByUrl(url: String): Flow<Podcast>

    @Query("SELECT * FROM episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    suspend fun loadPodcastEpisodesStatic(podcastId: Long): List<Episode>

    @Update
    suspend fun updatePodcasts(vararg podcasts: Podcast)

    @Query("SELECT * FROM podcast WHERE subscribed = :subscribed ORDER BY feedTitle")
    suspend fun loadSubscribedPodcastsStatic(subscribed: Boolean): List<Podcast>

}