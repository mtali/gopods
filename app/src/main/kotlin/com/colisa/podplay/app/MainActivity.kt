package com.colisa.podplay.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.colisa.podplay.R
import com.colisa.podplay.app.ui.GoApp
import com.colisa.podplay.app.ui.rememberGoAppState
import com.colisa.podplay.core.navigation.Navigator
import com.colisa.podplay.core.navigation.rememberNavigationState
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.core.ui.theme.GoPodsTheme
import com.colisa.podplay.core.ui.theme.shouldUseDarkTheme
import com.colisa.podplay.features.library.navigation.LibraryNavKey
import com.colisa.podplay.features.podcast_details.navigation.navigateToPodcastDetails
import com.colisa.podplay.sync.EpisodeUpdateWorker
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Width at which the navigation bar becomes a rail. */
private const val EXPANDED_WIDTH_DP = 600

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var playerConnection: PlayerConnection

  private val viewModel: MainViewModel by viewModels()

  private val appUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }

  private val appUpdateResultLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
      when (val resultCode = result.resultCode) {
        Activity.RESULT_OK -> quickMessage(getString(R.string.app_updated))

        Activity.RESULT_CANCELED -> {
          quickMessage(getString(R.string.app_update_required))
          lifecycleScope.launch {
            delay(10_000)
            checkForAppUpdates()
          }
        }

        else -> {
          quickMessage(getString(R.string.app_update_failed))
          Timber.e("Update flow failed with resultCode:$resultCode")
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    checkForAppUpdates()
    scheduleEpisodeUpdates()
    observePlayerErrors()

    val feedUrlFromNotification = intent?.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)

    setContent {
      val theme by viewModel.themeSettings.collectAsStateWithLifecycle()
      val playerState by viewModel.playerState.collectAsStateWithLifecycle()

      GoPodsTheme(
        darkTheme = shouldUseDarkTheme(theme.themeMode),
        dynamicColor = theme.useDynamicColor,
      ) {
        val navigationState = rememberNavigationState(LibraryNavKey)
        val navigator = remember(navigationState) { Navigator(navigationState) }
        val appState = rememberGoAppState(navigationState)

        // Opened from an episode update notification.
        LaunchedEffect(feedUrlFromNotification) {
          feedUrlFromNotification?.let { navigator.navigateToPodcastDetails(it) }
        }

        val widthDp = LocalConfiguration.current.screenWidthDp
        GoApp(
          appState = appState,
          navigator = navigator,
          playerState = playerState,
          onPlayPause = viewModel::onPlayPause,
          useNavigationRail = widthDp >= EXPANDED_WIDTH_DP,
        )
      }
    }
  }

  override fun onStart() {
    super.onStart()
    playerConnection.connect()
  }

  override fun onStop() {
    super.onStop()
    playerConnection.release()
  }

  override fun onResume() {
    super.onResume()
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
      if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
        appUpdateManager.startUpdateFlowForResult(
          info,
          appUpdateResultLauncher,
          AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
        )
      }
    }
  }

  private fun observePlayerErrors() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.playerErrors.collect { message -> quickMessage(message) }
      }
    }
  }

  private fun quickMessage(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  private fun scheduleEpisodeUpdates() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresCharging(true)
      .build()

    val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(1, TimeUnit.HOURS)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      TAG_EPISODE_UPDATE_JOB,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }

  private fun checkForAppUpdates() {
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
      val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
      val allowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
      if (available && allowed) {
        appUpdateManager.startUpdateFlowForResult(
          info,
          appUpdateResultLauncher,
          AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
        )
      }
    }
  }

  companion object {
    private const val TAG_EPISODE_UPDATE_JOB = "com.colisa.gopods.episodes"
  }
}
