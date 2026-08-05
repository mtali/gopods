package com.colisa.podplay.network.api

import com.colisa.podplay.network.models.PodcastResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface ItunesService {

    @GET("/search?media=podcast")
    suspend fun searchPodcasts(@Query("term") term: String): PodcastResponse

    companion object {
        val instance: ItunesService by lazy {
            val interceptor = HttpLoggingInterceptor().apply { setLevel(Level.BODY) }
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ItunesService::class.java)
        }
    }
}