package com.colisa.podplay.app.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.colisa.podplay.R
import com.colisa.podplay.core.navigation.NavigationState
import com.colisa.podplay.features.discover.navigation.DiscoverNavKey
import com.colisa.podplay.features.library.navigation.LibraryNavKey

/** Destinations shown in the navigation bar or rail. */
enum class TopLevelDestination(
  val key: NavKey,
  @param:StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  LIBRARY(
    key = LibraryNavKey,
    labelRes = R.string.library,
    selectedIcon = Icons.Filled.LibraryMusic,
    unselectedIcon = Icons.Outlined.LibraryMusic,
  ),
  DISCOVER(
    key = DiscoverNavKey,
    labelRes = R.string.discover,
    selectedIcon = Icons.Filled.Search,
    unselectedIcon = Icons.Outlined.Search,
  ),
}

@Composable
fun rememberGoAppState(navigationState: NavigationState): GoAppState {
  return remember(navigationState) { GoAppState(navigationState) }
}

@Stable
class GoAppState(val navigationState: NavigationState) {

  val currentTopLevelDestination: TopLevelDestination?
    get() = TopLevelDestination.entries.firstOrNull {
      it.key == navigationState.currentKey
    }

  /** The bar and the mini player only show on the top level destinations. */
  val showNavigationBar: Boolean
    get() = currentTopLevelDestination != null
}
