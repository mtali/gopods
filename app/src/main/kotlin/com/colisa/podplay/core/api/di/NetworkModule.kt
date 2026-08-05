package com.colisa.podplay.core.api.di

import com.colisa.podplay.BuildConfig
import com.colisa.podplay.core.api.ItunesApi
import com.colisa.podplay.core.common.ITUNES_BASE_URL
import com.colisa.podplay.core.common.JsonProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  private const val CONNECT_TIMEOUT_SECONDS = 30L
  private const val READ_TIMEOUT_SECONDS = 30L

  @Provides
  @Singleton
  fun okHttpCallFactory(): Call.Factory = OkHttpClient.Builder()
    .addInterceptor(
      HttpLoggingInterceptor().apply {
        if (BuildConfig.DEBUG) {
          setLevel(HttpLoggingInterceptor.Level.BASIC)
        }
      },
    )
    .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

  @Provides
  @Singleton
  fun providesNetworkJson(): Json = JsonProvider.json

  @Provides
  @Singleton
  fun providesItunesApi(callFactory: Call.Factory, networkJson: Json): ItunesApi =
    Retrofit.Builder()
      .baseUrl(ITUNES_BASE_URL)
      .callFactory(callFactory)
      .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
      .build()
      .create(ItunesApi::class.java)
}
