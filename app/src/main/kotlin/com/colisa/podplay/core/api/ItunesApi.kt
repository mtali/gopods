package com.colisa.podplay.core.api

import com.colisa.podplay.core.api.models.PodcastSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {

  @GET("search")
  suspend fun searchPodcasts(
    @Query("term") term: String,
    @Query("media") media: String = "podcast",
  ): PodcastSearchResponse
}
