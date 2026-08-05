package com.colisa.podplay.features.discover.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.features.discover.DiscoverRoute
import com.colisa.podplay.features.podcast_details.navigation.navigateToPodcastDetails
import kotlinx.serialization.Serializable

@Serializable
data object DiscoverNavKey : NavKey

fun Navigator.navigateToDiscover() = switchTopLevel(DiscoverNavKey)

fun EntryProviderScope<NavKey>.discoverEntry(navigator: Navigator) {
  entry<DiscoverNavKey> {
    DiscoverRoute(
      onPodcastClick = { feedUrl -> navigator.navigateToPodcastDetails(feedUrl) },
      viewModel = hiltViewModel(),
    )
  }
}
