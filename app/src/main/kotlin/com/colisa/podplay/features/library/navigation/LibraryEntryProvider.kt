package com.colisa.podplay.features.library.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.features.discover.navigation.navigateToDiscover
import com.colisa.podplay.features.library.LibraryRoute
import com.colisa.podplay.features.podcast_details.navigation.navigateToPodcastDetails
import com.colisa.podplay.features.settings.navigation.navigateToSettings
import kotlinx.serialization.Serializable

@Serializable
data object LibraryNavKey : NavKey

fun Navigator.navigateToLibrary() = switchTopLevel(LibraryNavKey)

fun EntryProviderScope<NavKey>.libraryEntry(navigator: Navigator) {
  entry<LibraryNavKey> {
    LibraryRoute(
      onPodcastClick = { feedUrl -> navigator.navigateToPodcastDetails(feedUrl) },
      onDiscoverClick = { navigator.navigateToDiscover() },
      onSettingsClick = { navigator.navigateToSettings() },
      viewModel = hiltViewModel(),
    )
  }
}
