package com.colisa.podplay.core.data.utils

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
  /** Kept as state so a repository can read it without suspending. */
  val isOnline: StateFlow<Boolean>
}
