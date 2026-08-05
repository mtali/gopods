package com.colisa.podplay.core.datastore

import com.colisa.podplay.core.models.NowPlayingEpisode

enum class ThemeMode {
  LIGHT, DARK, SYSTEM
}

data class UserPreferences(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = true,
  val notifyNewEpisodes: Boolean = true,
  val fastSeekSeconds: Int = DEFAULT_FAST_SEEK_SECONDS,
  val lastEpisode: NowPlayingEpisode? = null,
)

const val DEFAULT_FAST_SEEK_SECONDS = 10
const val MIN_FAST_SEEK_SECONDS = 5
const val MAX_FAST_SEEK_SECONDS = 60
