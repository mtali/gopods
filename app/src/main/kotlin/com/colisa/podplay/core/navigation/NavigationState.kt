package com.colisa.podplay.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
fun rememberNavigationState(startKey: NavKey): NavigationState {
  val backStack = rememberNavBackStack(startKey)
  return remember(startKey) {
    NavigationState(startKey = startKey, backStack = backStack)
  }
}

class NavigationState(val startKey: NavKey, val backStack: NavBackStack<NavKey>) {
  val currentKey by derivedStateOf { backStack.lastOrNull() ?: startKey }
}
