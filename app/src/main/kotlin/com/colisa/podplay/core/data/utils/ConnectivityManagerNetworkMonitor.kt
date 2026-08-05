package com.colisa.podplay.core.data.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.colisa.podplay.core.dispatchers.ApplicationScope
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.IO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityManagerNetworkMonitor @Inject constructor(
  @param:ApplicationContext private val context: Context,
  @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
  @ApplicationScope scope: CoroutineScope,
) : NetworkMonitor {

  override val isOnline: StateFlow<Boolean> = callbackFlow {
    val connectivityManager = context.getSystemService<ConnectivityManager>()
    if (connectivityManager == null) {
      channel.trySend(false)
      channel.close()
      return@callbackFlow
    }

    // The callback fires for any network matching the request, not just the active
    // one, so tracking the set of available networks is enough.
    val callback = object : NetworkCallback() {
      private val networks = mutableSetOf<Network>()

      override fun onAvailable(network: Network) {
        networks += network
        channel.trySend(true)
      }

      override fun onLost(network: Network) {
        networks -= network
        channel.trySend(networks.isNotEmpty())
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    connectivityManager.registerNetworkCallback(request, callback)

    channel.trySend(connectivityManager.isCurrentlyConnected())

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
  }
    .flowOn(ioDispatcher)
    .conflate()
    .stateIn(
      scope = scope,
      started = SharingStarted.Eagerly,
      initialValue = true,
    )

  private fun ConnectivityManager.isCurrentlyConnected(): Boolean {
    return activeNetwork
      ?.let(::getNetworkCapabilities)
      ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
  }
}
