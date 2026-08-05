package com.colisa.podplay.core.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.colisa.podplay.core.database.models.EpisodeEntity
import com.colisa.podplay.core.database.models.PodcastEntity
import com.colisa.podplay.core.database.models.PodcastSearchResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface PodcastDao {

  @Query("SELECT * FROM Podcast WHERE subscribed = :subscribed ORDER BY feedTitle")
  fun getPodcasts(subscribed: Boolean = true): Flow<List<PodcastEntity>>

  @Query("SELECT * FROM Podcast WHERE subscribed = :subscribed ORDER BY feedTitle")
  suspend fun loadSubscribedPodcasts(subscribed: Boolean): List<PodcastEntity>

  @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
  suspend fun getPodcast(url: String): PodcastEntity?

  @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
  fun loadPodcastByUrl(url: String): Flow<PodcastEntity?>

  @Query("SELECT * FROM Podcast WHERE collectionId IN (:collectionIds)")
  fun loadPodcastsById(collectionIds: List<Long>): Flow<List<PodcastEntity>>

  @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
  fun loadEpisodes(podcastId: Long): Flow<List<EpisodeEntity>>

  @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
  suspend fun loadEpisodesOnce(podcastId: Long): List<EpisodeEntity>

  @Query("SELECT * FROM PodcastSearchResult WHERE term = :term")
  suspend fun loadSearchResult(term: String): PodcastSearchResultEntity?

  /**
   * Search terms, most recent first. Rows are inserted with REPLACE, so searching a
   * term again gives it a new rowid and moves it to the top.
   */
  @Query("SELECT term FROM PodcastSearchResult ORDER BY rowid DESC LIMIT :limit")
  fun recentSearchTerms(limit: Int): Flow<List<String>>

  @Insert(onConflict = REPLACE)
  suspend fun insertPodcast(podcast: PodcastEntity): Long

  @Insert(onConflict = REPLACE)
  suspend fun insertPodcasts(podcasts: List<PodcastEntity>)

  @Insert(onConflict = REPLACE)
  suspend fun insertEpisode(episode: EpisodeEntity): Long

  @Insert(onConflict = REPLACE)
  suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

  @Insert(onConflict = REPLACE)
  suspend fun insertSearchResult(result: PodcastSearchResultEntity)

  @Update
  suspend fun updatePodcasts(vararg podcasts: PodcastEntity)

  @Delete
  suspend fun deletePodcast(podcast: PodcastEntity)

  /**
   * iTunes returns results by relevance, which SQL cannot reproduce, so the
   * original order is reapplied here.
   */
  fun loadPodcastsInSearchOrder(collectionIds: List<Long>): Flow<List<PodcastEntity>> {
    val position = collectionIds.withIndex().associate { (index, id) -> id to index }
    return loadPodcastsById(collectionIds).map { podcasts ->
      podcasts.sortedBy { position[it.collectionId] ?: Int.MAX_VALUE }
    }
  }
}
