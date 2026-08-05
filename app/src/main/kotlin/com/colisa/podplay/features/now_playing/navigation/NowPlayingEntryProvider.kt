package com.colisa.podplay.features.now_playing.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.features.now_playing.NowPlayingRoute
import kotlinx.serialization.Serializable

@Serializable
data object NowPlayingNavKey : NavKey

fun Navigator.navigateToNowPlaying() = navigate(NowPlayingNavKey)

fun EntryProviderScope<NavKey>.nowPlayingEntry(navigator: Navigator) {
  entry<NowPlayingNavKey> {
    NowPlayingRoute(
      onBackClick = navigator::goBack,
      viewModel = hiltViewModel(),
    )
  }
}
