package com.colisa.podplay.repository

import com.colisa.podplay.network.Result
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.network.models.PodcastResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject


class RealItunesRepo @Inject constructor(
    private val itunesService: ItunesService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ItunesRepo {
    override suspend fun searchPodcasts(term: String) = flow {
        emit(Result.Loading)
        try {
            val res = itunesService.searchItunesPodcast(term)
            if (res.isSuccessful) {
                emit(Result.OK(res.body()))
            } else {
                emit(Result.Error(Exception("Error: ${res.code()}")))
            }
        } catch (e: Throwable) {
            emit(Result.Error(e))
        }
    }.flowOn(ioDispatcher)


}


interface ItunesRepo {
    suspend fun searchPodcasts(term: String): Flow<Result<PodcastResponse?>>
}