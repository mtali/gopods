package com.colisa.podplay.core.data.di

import com.colisa.podplay.core.data.repository.ItunesRepository
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.data.repository.impl.ItunesRepositoryImpl
import com.colisa.podplay.core.data.repository.impl.PodcastRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract fun bindsItunesRepository(repository: ItunesRepositoryImpl): ItunesRepository

  @Binds
  @Singleton
  abstract fun bindsPodcastRepository(repository: PodcastRepositoryImpl): PodcastRepository
}
