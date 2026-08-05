package com.colisa.podplay.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.IO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val USER_PREFERENCES_NAME = "user_preferences"

// Keys written by the androidx.preference screen this replaces.
private const val LEGACY_THEME = "theme_pref"
private const val LEGACY_NOTIFY = "episode_notify"
private const val LEGACY_FAST_SEEK = "pref_fast_seeking"
private const val LEGACY_LAST_EPISODE = "pref_restore_latest_episode"

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

  @Provides
  @Singleton
  fun providePreferences(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
  ): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
      corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
      ),
      migrations = listOf(legacyPreferencesMigration(context)),
      scope = CoroutineScope(ioDispatcher + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
    )
  }

  /**
   * Carries settings across from the default SharedPreferences file used before this
   * version, so an existing install keeps its theme, notification and seek choices
   * along with the last played episode. The keys are renamed on the way in.
   */
  private fun legacyPreferencesMigration(
    context: Context,
  ): SharedPreferencesMigration<Preferences> {
    val legacyName = "${context.packageName}_preferences"
    return SharedPreferencesMigration(
      produceSharedPreferences = {
        context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
      },
      keysToMigrate = setOf(
        LEGACY_THEME,
        LEGACY_NOTIFY,
        LEGACY_FAST_SEEK,
        LEGACY_LAST_EPISODE,
      ),
    ) { legacy, current ->
      val updated = current.toMutablePreferences()
      legacy.getAll().forEach { (key, value) ->
        when (key) {
          LEGACY_THEME -> (value as? String)?.let {
            updated[stringPreferencesKey("theme_mode")] = it
          }

          LEGACY_NOTIFY -> (value as? Boolean)?.let {
            updated[booleanPreferencesKey("notify_new_episodes")] = it
          }

          LEGACY_FAST_SEEK -> (value as? Int)?.let {
            updated[intPreferencesKey("fast_seek_seconds")] = it
          }

          LEGACY_LAST_EPISODE -> (value as? String)?.let {
            updated[stringPreferencesKey("last_episode")] = it
          }
        }
      }
      updated.toPreferences()
    }
  }
}
