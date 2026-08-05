package com.colisa.podplay.core.database.di

import android.content.Context
import com.colisa.podplay.core.database.GoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Singleton
  @Provides
  fun provideDatabase(@ApplicationContext context: Context): GoDatabase {
    return GoDatabase.getInstance(context)
  }
}
