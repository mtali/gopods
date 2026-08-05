package com.colisa.podplay.core.data

import com.colisa.podplay.core.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Emits the cached value first, then refreshes from the network and emits again.
 * On failure the cached value is kept and returned inside [Result.Error].
 */
inline fun <ResultType, RequestType> networkBoundResource(
  crossinline query: () -> Flow<ResultType>,
  crossinline fetch: suspend () -> RequestType,
  crossinline saveFetchResult: suspend (RequestType) -> Unit,
  crossinline shouldFetch: (ResultType) -> Boolean = { true },
  crossinline onFetchFailed: (Throwable) -> Unit = {},
): Flow<Result<ResultType>> = flow {
  val cached = query().first()
  if (shouldFetch(cached)) {
    emit(Result.Loading(cached))
    try {
      saveFetchResult(fetch())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      onFetchFailed(e)
      emitAll(query().map { Result.Error(e, it) })
      return@flow
    }
  }
  emitAll(query().map { Result.Success(it) })
}
