package com.colisa.podplay.core.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.colisa.podplay.core.database.models.EpisodeEntity
import com.colisa.podplay.core.database.models.PodcastEntity
import com.colisa.podplay.core.database.models.PodcastSearchResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

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

  /**
   * Returns -1 when the podcast is already stored. REPLACE is deliberately not used:
   * the unique collectionId index would make it delete the existing row, taking the
   * subscription and, through the foreign key cascade, the episodes with it.
   */
  @Insert(onConflict = IGNORE)
  suspend fun insertPodcastIfAbsent(podcast: PodcastEntity): Long

  /** The fields iTunes owns. Everything else on the row belongs to the user. */
  @Query(
    """
    UPDATE Podcast SET
      feedUrl = :feedUrl,
      feedTitle = :feedTitle,
      imageUrl = :imageUrl,
      imageUrl600 = :imageUrl600,
      lastUpdated = :lastUpdated
    WHERE collectionId = :collectionId
    """
  )
  suspend fun updateSearchFields(
    collectionId: Long,
    feedUrl: String,
    feedTitle: String,
    imageUrl: String,
    imageUrl600: String,
    lastUpdated: Date,
  )

  @Query("UPDATE Podcast SET feedDescription = :description WHERE id = :id")
  suspend fun updateFeedDescription(id: Long, description: String)

  @Query("UPDATE Podcast SET subscribed = :subscribed WHERE id = :id")
  suspend fun updateSubscribed(id: Long, subscribed: Boolean)

  @Insert(onConflict = REPLACE)
  suspend fun insertEpisode(episode: EpisodeEntity): Long

  @Insert(onConflict = REPLACE)
  suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

  @Insert(onConflict = REPLACE)
  suspend fun insertSearchResult(result: PodcastSearchResultEntity)

  /**
   * Stores a search hit without disturbing a podcast that is already known, so
   * searching a term again cannot drop a subscription or its episodes.
   */
  suspend fun upsertSearchResult(podcast: PodcastEntity) {
    if (insertPodcastIfAbsent(podcast) != -1L) return
    updateSearchFields(
      collectionId = podcast.collectionId,
      feedUrl = podcast.feedUrl,
      feedTitle = podcast.feedTitle,
      imageUrl = podcast.imageUrl,
      imageUrl600 = podcast.imageUrl600,
      lastUpdated = podcast.lastUpdated,
    )
  }

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
