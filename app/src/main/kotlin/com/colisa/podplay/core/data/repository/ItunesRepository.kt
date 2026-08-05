package com.colisa.podplay.core.data.repository

import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.models.Podcast
import kotlinx.coroutines.flow.Flow

interface ItunesRepository {
  fun searchPodcasts(term: String): Flow<Result<List<Podcast>>>

  fun recentSearches(limit: Int = DEFAULT_RECENT_SEARCHES): Flow<List<String>>
}

const val DEFAULT_RECENT_SEARCHES = 8
