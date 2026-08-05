package com.colisa.podplay.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.colisa.podplay.core.common.JsonProvider
import com.colisa.podplay.core.datastore.PreferencesKeys.DYNAMIC_COLOR
import com.colisa.podplay.core.datastore.PreferencesKeys.FAST_SEEK_SECONDS
import com.colisa.podplay.core.datastore.PreferencesKeys.LAST_EPISODE
import com.colisa.podplay.core.datastore.PreferencesKeys.NOTIFY_NEW_EPISODES
import com.colisa.podplay.core.datastore.PreferencesKeys.THEME_MODE
import com.colisa.podplay.core.models.NowPlayingEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private object PreferencesKeys {
  val THEME_MODE = stringPreferencesKey("theme_mode")
  val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
  val NOTIFY_NEW_EPISODES = booleanPreferencesKey("notify_new_episodes")
  val FAST_SEEK_SECONDS = intPreferencesKey("fast_seek_seconds")
  val LAST_EPISODE = stringPreferencesKey("last_episode")
}

/** Reads and writes user settings and the last played episode. */
@Singleton
class PreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

  private val json: Json = JsonProvider.json

  val userPreferences: Flow<UserPreferences> = dataStore.data
    .catch { exception ->
      if (exception is IOException) {
        Timber.e(exception, "Failed to read preferences")
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }
    .map { preferences ->
      UserPreferences(
        themeMode = preferences[THEME_MODE]?.toThemeMode() ?: ThemeMode.SYSTEM,
        useDynamicColor = preferences[DYNAMIC_COLOR] ?: true,
        notifyNewEpisodes = preferences[NOTIFY_NEW_EPISODES] ?: true,
        fastSeekSeconds = preferences[FAST_SEEK_SECONDS] ?: DEFAULT_FAST_SEEK_SECONDS,
        lastEpisode = preferences[LAST_EPISODE]?.let(::decodeEpisode),
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { it[THEME_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    dataStore.edit { it[DYNAMIC_COLOR] = enabled }
  }

  suspend fun setNotifyNewEpisodes(enabled: Boolean) {
    dataStore.edit { it[NOTIFY_NEW_EPISODES] = enabled }
  }

  suspend fun setFastSeekSeconds(seconds: Int) {
    dataStore.edit {
      it[FAST_SEEK_SECONDS] = seconds.coerceIn(MIN_FAST_SEEK_SECONDS, MAX_FAST_SEEK_SECONDS)
    }
  }

  suspend fun setLastEpisode(episode: NowPlayingEpisode) {
    dataStore.edit { it[LAST_EPISODE] = json.encodeToString(episode) }
  }

  private fun decodeEpisode(value: String): NowPlayingEpisode? = try {
    json.decodeFromString<NowPlayingEpisode>(value)
  } catch (e: Exception) {
    Timber.e(e, "Failed to read the stored episode")
    null
  }

  private fun String.toThemeMode(): ThemeMode = when (this) {
    // Values written by the old androidx.preference screen.
    "theme_pref_light" -> ThemeMode.LIGHT
    "theme_pref_dark" -> ThemeMode.DARK
    "theme_pref_auto" -> ThemeMode.SYSTEM
    else -> runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.SYSTEM)
  }
}
