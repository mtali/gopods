package com.colisa.podplay.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.core.datastore.PreferencesDataSource
import com.colisa.podplay.core.datastore.ThemeMode
import com.colisa.podplay.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val preferencesDataSource: PreferencesDataSource,
) : ViewModel() {

  val uiState: StateFlow<UserPreferences> = preferencesDataSource.userPreferences
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )

  fun onThemeModeChange(mode: ThemeMode) {
    viewModelScope.launch { preferencesDataSource.setThemeMode(mode) }
  }

  fun onDynamicColorChange(enabled: Boolean) {
    viewModelScope.launch { preferencesDataSource.setUseDynamicColor(enabled) }
  }

  fun onNotifyNewEpisodesChange(enabled: Boolean) {
    viewModelScope.launch { preferencesDataSource.setNotifyNewEpisodes(enabled) }
  }

  fun onFastSeekSecondsChange(seconds: Int) {
    viewModelScope.launch { preferencesDataSource.setFastSeekSeconds(seconds) }
  }
}
