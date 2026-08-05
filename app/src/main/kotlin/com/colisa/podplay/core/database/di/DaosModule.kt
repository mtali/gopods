package com.colisa.podplay.core.database.di

import com.colisa.podplay.core.database.GoDatabase
import com.colisa.podplay.core.database.daos.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaosModule {
  @Singleton
  @Provides
  fun providePodcastDao(database: GoDatabase): PodcastDao = database.podcastDao()
}
