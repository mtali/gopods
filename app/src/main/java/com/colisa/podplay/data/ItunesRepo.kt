package com.colisa.podplay.data

import com.colisa.podplay.db.GoDatabase
import com.colisa.podplay.db.PodcastDao
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.models.PodcastSearchResult
import com.colisa.podplay.network.Resource
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.network.networkBoundResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject


class RealItunesRepo @Inject constructor(
    private val itunesService: ItunesService,
    private val podcastDao: PodcastDao,
    private val db: GoDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ItunesRepo {

    override fun searchPodcasts(term: String) = networkBoundResource(
        query = {
            val search = podcastDao.loadSearchResult(term)
            podcastDao.loadPodcastOrdered(search?.collectionIds ?: emptyList())
        },

        fetch = {
            itunesService.searchPodcasts(term)
        },

        saveFetchResult = { response ->
            val ids = response.results.map { it.collectionId }
            val searchResult = PodcastSearchResult(term, ids, response.resultCount)
            val pods = response.toPodcasts()

            db.runInTransaction {
                podcastDao.insertPodcasts(pods)
                podcastDao.insertSearchResult(searchResult)
            }
        },

        shouldFetch = { true }

    ).flowOn(ioDispatcher)

}


interface ItunesRepo {
    fun searchPodcasts(term: String): Flow<Resource<out List<Podcast>>>
}