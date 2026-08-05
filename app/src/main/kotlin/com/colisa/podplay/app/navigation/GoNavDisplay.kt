package com.colisa.podplay.app.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.colisa.podplay.BuildConfig
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.core.ui.components.AppLoading
import com.colisa.podplay.features.discover.navigation.discoverEntry
import com.colisa.podplay.features.library.navigation.libraryEntry
import com.colisa.podplay.features.now_playing.navigation.nowPlayingEntry
import com.colisa.podplay.features.podcast_details.navigation.podcastDetailsEntry
import com.colisa.podplay.features.settings.navigation.settingsEntry
import timber.log.Timber

@Composable
fun GoNavDisplay(
  navigator: Navigator,
  modifier: Modifier = Modifier,
) {
  LogBackStack(navigator)

  val entryProvider = entryProvider {
    libraryEntry(navigator)
    discoverEntry(navigator)
    podcastDetailsEntry(navigator)
    nowPlayingEntry(navigator)
    settingsEntry(navigator)
  }

  // Safety: NavDisplay crashes if the back stack is empty
  if (navigator.state.backStack.isNotEmpty()) {
    NavDisplay(
      modifier = modifier,
      entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
      backStack = navigator.state.backStack,
      onBack = { navigator.goBack() },
      entryProvider = entryProvider,
      transitionSpec = {
        slideInHorizontally(initialOffsetX = { it }) togetherWith
          slideOutHorizontally(targetOffsetX = { -it })
      },
      popTransitionSpec = {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
          slideOutHorizontally(targetOffsetX = { it })
      },
      predictivePopTransitionSpec = {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
          slideOutHorizontally(targetOffsetX = { it })
      },
    )
  } else {
    Timber.w("NavDisplay: back stack is empty")
    AppLoading(modifier = Modifier.fillMaxSize())
  }
}

@Composable
private fun LogBackStack(navigator: Navigator) {
  if (BuildConfig.DEBUG) {
    LaunchedEffect(navigator.state.backStack.toList()) {
      val stack = navigator.state.backStack.joinToString(" -> ") {
        it::class.simpleName ?: it.toString()
      }
      Timber.tag(Navigator.TAG).d("[BackStack] $stack")
    }
  }
}
