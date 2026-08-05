package com.colisa.podplay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.colisa.podplay.BuildConfig
import com.colisa.podplay.core.logs.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Call
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class GoApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  /** Shared with the api layer so artwork and feeds use one connection pool. */
  @Inject
  lateinit var callFactory: Call.Factory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    } else {
      Timber.plant(ReleaseTree())
    }
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
      .components {
        add(OkHttpNetworkFetcherFactory(callFactory = { callFactory }))
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("image_cache"))
          .build()
      }
      .crossfade(true)
      .build()
  }
}
