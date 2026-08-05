package com.colisa.podplay.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.core.datastore.PreferencesDataSource
import com.colisa.podplay.core.datastore.ThemeMode
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.core.player.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeSettings(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = true,
)

@HiltViewModel
class MainViewModel @Inject constructor(
  private val playerConnection: PlayerConnection,
  preferencesDataSource: PreferencesDataSource,
) : ViewModel() {

  val themeSettings: StateFlow<ThemeSettings> = preferencesDataSource.userPreferences
    .map { ThemeSettings(it.themeMode, it.useDynamicColor) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = ThemeSettings(),
    )

  val playerState: StateFlow<PlayerUiState> = playerConnection.state

  val playerErrors: SharedFlow<String> = playerConnection.errors

  fun onPlayPause() = playerConnection.togglePlayPause()
}
