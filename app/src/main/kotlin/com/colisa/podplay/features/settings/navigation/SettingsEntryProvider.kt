package com.colisa.podplay.features.settings.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.features.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsNavKey : NavKey

fun Navigator.navigateToSettings() = navigate(SettingsNavKey)

fun EntryProviderScope<NavKey>.settingsEntry(navigator: Navigator) {
  entry<SettingsNavKey> {
    SettingsRoute(
      onBackClick = navigator::goBack,
      viewModel = hiltViewModel(),
    )
  }
}
