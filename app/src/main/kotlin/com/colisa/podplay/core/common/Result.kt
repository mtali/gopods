package com.colisa.podplay.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

/**
 * Loading and Error both carry [data] so a screen can keep showing cached content
 * while a refresh runs, or after it fails.
 */
sealed interface Result<out T> {
  val data: T?

  data class Success<out T>(override val data: T) : Result<T>
  data class Loading<out T>(override val data: T? = null) : Result<T>
  data class Error<out T>(
    val exception: Throwable? = null,
    override val data: T? = null,
  ) : Result<T>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
  return this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading()) }
    .catch {
      Timber.e(it)
      emit(Result.Error(it))
    }
}
