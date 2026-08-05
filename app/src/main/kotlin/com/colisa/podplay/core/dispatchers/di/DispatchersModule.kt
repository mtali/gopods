package com.colisa.podplay.core.dispatchers.di

import com.colisa.podplay.core.dispatchers.ApplicationScope
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.Default
import com.colisa.podplay.core.dispatchers.GoDispatchers.IO
import com.colisa.podplay.core.dispatchers.GoDispatchers.Main
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
  @Provides
  @Dispatcher(IO)
  fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @Dispatcher(Default)
  fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

  @Provides
  @Dispatcher(Main)
  fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

  @Provides
  @Singleton
  @ApplicationScope
  fun provideAppCoroutineScope(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
  ): CoroutineScope {
    val handler = CoroutineExceptionHandler { _, e ->
      Timber.e(e, "Uncaught exception in ApplicationScope")
    }
    return CoroutineScope(SupervisorJob() + ioDispatcher + handler)
  }
}
