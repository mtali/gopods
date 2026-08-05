package com.colisa.podplay.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.app.navigation.GoNavDisplay
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.core.player.PlayerUiState
import com.colisa.podplay.core.ui.components.MiniPlayer
import com.colisa.podplay.features.now_playing.navigation.navigateToNowPlaying

@Composable
fun GoApp(
  appState: GoAppState,
  navigator: Navigator,
  playerState: PlayerUiState,
  onPlayPause: () -> Unit,
  useNavigationRail: Boolean,
) {
  Scaffold(
    bottomBar = {
      if (appState.showNavigationBar && !useNavigationRail) {
        Column {
          MiniPlayer(
            state = playerState,
            onPlayPause = onPlayPause,
            onOpen = { navigator.navigateToNowPlaying() },
          )
          GoNavigationBar(appState = appState, navigator = navigator)
        }
      }
    },
  ) { padding ->
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .consumeWindowInsets(padding),
    ) {
      if (appState.showNavigationBar && useNavigationRail) {
        GoNavigationRail(appState = appState, navigator = navigator)
      }
      Column(modifier = Modifier.fillMaxSize()) {
        GoNavDisplay(navigator = navigator, modifier = Modifier.weight(1f))
        if (appState.showNavigationBar && useNavigationRail) {
          MiniPlayer(
            state = playerState,
            onPlayPause = onPlayPause,
            onOpen = { navigator.navigateToNowPlaying() },
          )
        }
      }
    }
  }
}

@Composable
private fun GoNavigationBar(appState: GoAppState, navigator: Navigator) {
  val current = appState.currentTopLevelDestination
  NavigationBar {
    TopLevelDestination.entries.forEach { destination ->
      val selected = destination == current
      NavigationBarItem(
        selected = selected,
        onClick = { navigator.switchTopLevel(destination.key) },
        icon = {
          Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = null,
          )
        },
        label = { Text(stringResource(destination.labelRes)) },
      )
    }
  }
}

@Composable
private fun GoNavigationRail(appState: GoAppState, navigator: Navigator) {
  val current = appState.currentTopLevelDestination
  NavigationRail(
    header = {
      Icon(imageVector = Icons.Filled.Podcasts, contentDescription = null)
    },
  ) {
    TopLevelDestination.entries.forEach { destination ->
      val selected = destination == current
      NavigationRailItem(
        selected = selected,
        onClick = { navigator.switchTopLevel(destination.key) },
        icon = {
          Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = null,
          )
        },
        label = { Text(stringResource(destination.labelRes)) },
      )
    }
  }
}
