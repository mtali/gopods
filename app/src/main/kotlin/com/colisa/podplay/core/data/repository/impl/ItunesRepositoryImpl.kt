package com.colisa.podplay.core.data.repository.impl

import androidx.room.withTransaction
import com.colisa.podplay.core.api.ItunesApi
import com.colisa.podplay.core.api.models.asPodcasts
import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.data.networkBoundResource
import com.colisa.podplay.core.data.repository.ItunesRepository
import com.colisa.podplay.core.database.GoDatabase
import com.colisa.podplay.core.database.daos.PodcastDao
import com.colisa.podplay.core.database.models.PodcastSearchResultEntity
import com.colisa.podplay.core.database.models.asEntity
import com.colisa.podplay.core.database.models.asPodcast
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.IO
import com.colisa.podplay.core.models.Podcast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItunesRepositoryImpl @Inject constructor(
  private val itunesApi: ItunesApi,
  private val podcastDao: PodcastDao,
  private val database: GoDatabase,
  @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ItunesRepository {

  override fun searchPodcasts(term: String): Flow<Result<List<Podcast>>> = networkBoundResource(
    query = {
      // The stored result holds the collection ids for this term, so it has to be
      // read before the podcast rows can be looked up.
      flow {
        val search = podcastDao.loadSearchResult(term)
        val ids = search?.collectionIds ?: emptyList()
        emitAll(
          podcastDao.loadPodcastsInSearchOrder(ids).map { entities ->
            entities.map { it.asPodcast() }
          },
        )
      }
    },
    fetch = {
      itunesApi.searchPodcasts(term)
    },
    saveFetchResult = { response ->
      val podcasts = response.asPodcasts()
      val searchResult = PodcastSearchResultEntity(
        term = term,
        collectionIds = podcasts.map { it.collectionId },
        count = response.resultCount,
      )
      database.withTransaction {
        podcastDao.insertPodcasts(podcasts.map { it.asEntity() })
        podcastDao.insertSearchResult(searchResult)
      }
    },
  ).flowOn(ioDispatcher)
}
