package com.colisa.podplay.core.navigation

import androidx.navigation3.runtime.NavKey
import timber.log.Timber

class Navigator(val state: NavigationState) {

  fun navigate(key: NavKey, pop: Boolean = false) {
    state.backStack.apply {
      val index = indexOf(key)
      if (index >= 0) {
        val from = if (pop) index else index + 1
        if (from < size) {
          // Safety: never clear index 0 to avoid a NavDisplay crash
          subList(maxOf(from, 1), size).clear()
        }
      }

      if (!contains(key)) add(key)
    }
  }

  /** Switches the top level destination, keeping a single entry at the root. */
  fun switchTopLevel(key: NavKey) {
    state.backStack.apply {
      if (size > 1) subList(1, size).clear()
      if (firstOrNull() != key) {
        add(0, key)
        if (size > 1) subList(1, size).clear()
      }
    }
  }

  fun goBack() {
    // Safety: NavDisplay crashes if size becomes 0.
    if (state.backStack.size > 1) {
      state.backStack.removeLastOrNull()
    } else {
      Timber.tag(TAG).w("Can't go back: stack size is 1")
    }
  }

  companion object {
    const val TAG = "Navigator"
  }
}
