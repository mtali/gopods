package com.colisa.podplay.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.colisa.podplay.BuildConfig
import com.colisa.podplay.GoPreferences
import com.colisa.podplay.logging.ReleaseTree
import com.colisa.podplay.util.ThemeUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class GoApp : Application(), Configuration.Provider {

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .build()

  override fun onCreate() {
    super.onCreate()
    setTimber()
    prefs = GoPreferences(applicationContext)
    AppCompatDelegate.setDefaultNightMode(ThemeUtils.getDefaultNightMode(applicationContext))
  }

  private fun setTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    } else {
      Timber.plant(ReleaseTree())
    }
  }

  companion object {
    lateinit var prefs: GoPreferences
  }
}

/**
 * Still a global because the androidx.preference settings screen reads and writes
 * SharedPreferences directly. Replaced by a DataStore backed source when that
 * screen becomes Compose.
 */
val goPreferences: GoPreferences by lazy { GoApp.prefs }
