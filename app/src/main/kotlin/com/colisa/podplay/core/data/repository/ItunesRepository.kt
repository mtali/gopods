package com.colisa.podplay.core.data.repository

import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.models.Podcast
import kotlinx.coroutines.flow.Flow

interface ItunesRepository {
  fun searchPodcasts(term: String): Flow<Result<List<Podcast>>>
}
