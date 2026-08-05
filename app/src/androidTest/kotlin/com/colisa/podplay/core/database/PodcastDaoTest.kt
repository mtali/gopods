package com.colisa.podplay.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.colisa.podplay.core.database.daos.PodcastDao
import com.colisa.podplay.core.database.models.EpisodeEntity
import com.colisa.podplay.core.database.models.PodcastEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * A search hit carries no id and subscribed = false, so writing one over a podcast that
 * is already stored must not take the subscription or the episodes with it.
 */
@RunWith(AndroidJUnit4::class)
class PodcastDaoTest {

  private lateinit var database: GoDatabase
  private lateinit var dao: PodcastDao

  private val searchHit = PodcastEntity(
    collectionId = 42,
    feedUrl = "https://example.com/feed.xml",
    feedTitle = "Example Show",
    imageUrl = "https://example.com/100.jpg",
    imageUrl600 = "https://example.com/600.jpg",
    lastUpdated = Date(1_700_000_000_000),
  )

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    database = Room.inMemoryDatabaseBuilder(context, GoDatabase::class.java).build()
    dao = database.podcastDao()
  }

  @After
  fun tearDown() = database.close()

  private fun episode(guid: String, podcastId: Long) =
    EpisodeEntity(guid = guid, podcastId = podcastId, title = guid)

  /** Discover re-collects its search flow whenever it comes back into view. */
  @Test
  fun searchingAgainKeepsTheSubscriptionAndEpisodes() = runBlocking {
    dao.upsertSearchResult(searchHit)
    val id = requireNotNull(dao.getPodcast(searchHit.feedUrl)?.id)
    dao.insertEpisodes(listOf(episode("guid-1", id), episode("guid-2", id)))
    dao.updateFeedDescription(id, "From the feed")
    dao.updateSubscribed(id, subscribed = true)

    dao.upsertSearchResult(searchHit.copy(feedTitle = "Example Show (renamed)"))

    val stored = requireNotNull(dao.getPodcast(searchHit.feedUrl))
    assertEquals(id, stored.id)
    assertTrue(stored.subscribed)
    assertEquals("From the feed", stored.feedDescription)
    assertEquals(2, dao.loadEpisodesOnce(id).size)
    // iTunes still owns the title, so a rename does come through.
    assertEquals("Example Show (renamed)", stored.feedTitle)
  }

  @Test
  fun subscribedPodcastReachesTheLibrary() = runBlocking {
    dao.upsertSearchResult(searchHit)
    val id = requireNotNull(dao.getPodcast(searchHit.feedUrl)?.id)

    dao.updateSubscribed(id, subscribed = true)
    assertEquals(listOf(id), dao.loadSubscribedPodcasts(subscribed = true).map { it.id })

    dao.upsertSearchResult(searchHit)
    assertEquals(listOf(id), dao.loadSubscribedPodcasts(subscribed = true).map { it.id })

    dao.updateSubscribed(id, subscribed = false)
    assertTrue(dao.loadSubscribedPodcasts(subscribed = true).isEmpty())
  }

  /** A feed refresh only owns the description. */
  @Test
  fun refreshingTheFeedKeepsTheSubscription() = runBlocking {
    dao.upsertSearchResult(searchHit)
    val id = requireNotNull(dao.getPodcast(searchHit.feedUrl)?.id)
    dao.insertEpisodes(listOf(episode("guid-1", id)))
    dao.updateSubscribed(id, subscribed = true)

    dao.updateFeedDescription(id, "Refreshed description")

    val stored = requireNotNull(dao.getPodcast(searchHit.feedUrl))
    assertEquals("Refreshed description", stored.feedDescription)
    assertTrue(stored.subscribed)
    assertEquals(1, dao.loadEpisodesOnce(id).size)
  }

  /**
   * What the REPLACE this fix removed used to do. collectionId is uniquely indexed, so
   * SQLite deletes the stored row rather than merging into it, and the foreign key
   * cascade takes the episodes as well.
   */
  @Test
  fun replacingTheRowIsWhatDroppedTheSubscription() = runBlocking {
    dao.upsertSearchResult(searchHit)
    val id = requireNotNull(dao.getPodcast(searchHit.feedUrl)?.id)
    dao.insertEpisodes(listOf(episode("guid-1", id)))
    dao.updateSubscribed(id, subscribed = true)

    database.openHelper.writableDatabase.execSQL(
      """
      INSERT OR REPLACE INTO Podcast
        (collectionId, feedUrl, feedTitle, feedDescription, imageUrl, imageUrl600,
         lastUpdated, subscribed)
      VALUES (42, 'https://example.com/feed.xml', 'Example Show', '', '', '', 0, 0)
      """
    )

    val stored = requireNotNull(dao.getPodcast(searchHit.feedUrl))
    assertNotEquals(id, stored.id)
    assertFalse(stored.subscribed)
    assertTrue(dao.loadEpisodesOnce(id).isEmpty())
  }
}
