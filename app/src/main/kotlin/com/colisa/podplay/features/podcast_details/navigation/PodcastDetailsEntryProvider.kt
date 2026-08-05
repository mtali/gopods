package com.colisa.podplay.features.podcast_details.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.features.podcast_details.PodcastDetailsRoute
import com.colisa.podplay.features.podcast_details.PodcastDetailsViewModel
import kotlinx.serialization.Serializable

@Serializable
data class PodcastDetailsNavKey(val feedUrl: String) : NavKey

fun Navigator.navigateToPodcastDetails(feedUrl: String) =
  navigate(PodcastDetailsNavKey(feedUrl))

fun EntryProviderScope<NavKey>.podcastDetailsEntry(navigator: Navigator) {
  entry<PodcastDetailsNavKey> { key ->
    PodcastDetailsRoute(
      onBackClick = navigator::goBack,
      viewModel = hiltViewModel<PodcastDetailsViewModel, PodcastDetailsViewModel.Factory>(
        key = key.feedUrl
      ) { factory ->
        factory.create(key)
      },
    )
  }
}
