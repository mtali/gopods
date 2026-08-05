package com.colisa.podplay.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.colisa.podplay.R
import com.colisa.podplay.app.goPreferences
import com.colisa.podplay.core.models.NowPlayingEpisode
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.databinding.ActivityMainBinding
import com.colisa.podplay.databinding.NowPlayingBinding
import com.colisa.podplay.databinding.PlayerControlsPanelBinding
import com.colisa.podplay.extensions.afterMeasured
import com.colisa.podplay.extensions.setupMessagingToast
import com.colisa.podplay.sync.EpisodeUpdateWorker
import com.colisa.podplay.ui.fragments.OnPodcastDetailsListener
import com.colisa.podplay.util.EventObserver
import com.colisa.podplay.util.ThemeUtils
import com.colisa.podplay.util.VersionUtils
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnPodcastDetailsListener, UIControlInterface {

    @Inject
    lateinit var playerConnection: PlayerConnection

    // Binding classed
    private var binding: ActivityMainBinding? = null
    private var playerControlsPanelBinding: PlayerControlsPanelBinding? = null
    private var npBinding: NowPlayingBinding? = null

    // View model
    private val goViewModel: GoViewModel by viewModels()
    private val npViewModel: NowPlayingViewModel by viewModels()

    private lateinit var playingDialog: MaterialDialog

    private var draggingScrubber: Boolean = false

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }

    private val appUpdateResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (val resultCode = result.resultCode) {
                Activity.RESULT_OK -> {
                    quickMessage(R.string.app_updated)
                }

                Activity.RESULT_CANCELED -> {
                    quickMessage(R.string.app_update_required)
                    lifecycleScope.launch {
                        delay(10_000)
                        checkForAppUpdates()
                    }
                }

                else -> {
                    quickMessage(R.string.app_update_failed)
                    Timber.e("Update flow failed with resultCode:$resultCode")
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkForAppUpdates()
        setTheme(ThemeUtils.getAccentedTheme().first)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Toast messages
        binding!!.root.setupMessagingToast(this, goViewModel.snackbar)

        // Other bindings
        playerControlsPanelBinding = binding!!.playerControls
        setupBinding()

        // Support edge to edge
        if (VersionUtils.isOreoMR1()) {
            edgeToEdge {
                binding!!.root.fit { Edge.Top + Edge.Bottom }
            }
        }

        // Simple reveal animation
        synchronized(Any()) {
            binding!!.mainView.animate().apply {
                duration = 300
                alpha(1.0F)
            }
        }

        initMedia()
        scheduleJobs()
        handleIntent(intent)
        setObservers()
        setupControls()
    }


    private fun setupControls() {
        playerControlsPanelBinding?.let {
            it.playPauseButton.setOnClickListener {
                checkIsPlayer {
                    playerConnection.togglePlayPause()
                }
            }
        }
    }

    private fun checkIsPlayer(showError: Boolean = true, block: () -> Unit) {
        if (goPreferences.latestEpisode == null) {
            if (showError) {
                quickMessage(R.string.error_bad_episode)
            }
        } else {
            block.invoke()
        }
    }


    override fun onResume() {
        super.onResume()
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        appUpdateResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
    }


    private fun setObservers() {
        goViewModel.playEpisodeEvent.observe(this, EventObserver {
            onEpisodeSelected(it)
        })

        npViewModel.positionMs.observe(this) { position ->
            if (!draggingScrubber) {
                npBinding?.npSeekBar?.progress = position.toInt()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerConnection.errors.collect { message -> quickMessage(message) }
            }
        }
    }


    private fun onEpisodeSelected(episode: GoViewModel.REpisode) {
        val podcast = goViewModel.rPodcastFeed.value ?: return
        val npEpisode = NowPlayingEpisode(
            title = episode.title.orEmpty(),
            artUrl = podcast.imageUrl.orEmpty(),
            artUrl600 = podcast.imageUrl600.orEmpty(),
            mediaUrl = episode.mediaUrl.orEmpty(),
            description = episode.description.orEmpty(),
            podcastTitle = podcast.feedTitle.orEmpty(),
        )
        if (npEpisode.mediaUrl.isBlank()) {
            quickMessage(R.string.error_media_not_found)
            return
        }
        playerConnection.play(npEpisode)
        openNowPlaying()
    }

    override fun onStart() {
        super.onStart()
        playerConnection.connect()
    }

    override fun onStop() {
        super.onStop()
        playerConnection.release()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let { that ->
            val url = that.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
            if (url != null) {
                goViewModel.setActivePodcast(url)
            }
        }
    }

    private fun initMedia() {
        playerControlsPanelBinding?.let { playerBinding ->
            with(playerBinding.playingEpisodeContainer) {
                setOnClickListener {
                    checkIsPlayer {
                        openNowPlaying()
                    }
                }
            }
        }
    }

    private fun openNowPlaying() {
        playingDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            customView(R.layout.now_playing)
            npBinding = NowPlayingBinding.bind(getCustomView())
            npBinding?.let { bn ->
                bn.lifecycleOwner = this@MainActivity
                bn.npViewModel = npViewModel
                bn.npPlay.setOnClickListener { checkIsPlayer { playerConnection.togglePlayPause() } }
                bn.npFastForward.setOnClickListener { checkIsPlayer { playerConnection.seekBy(forward = true) } }
                bn.npFastRewind.setOnClickListener { checkIsPlayer { playerConnection.seekBy(forward = false) } }
            }

            if (VersionUtils.isOreoMR1() && !ThemeUtils.isDeviceLand(resources)) {
                edgeToEdge {
                    view.fit { Edge.Bottom }
                }
            }
            getCustomView().afterMeasured {
                playingDialog.setPeekHeight(height)
            }

            setupSeekBarProgressListener()

            onDismiss {
                npBinding?.npSeekBar?.setOnSeekBarChangeListener(null)
                npBinding = null
            }
        }
    }

    private fun setupBinding() {
        binding?.let {
            it.lifecycleOwner = this
            it.goViewModel = goViewModel
        }
        playerControlsPanelBinding?.let {
            it.lifecycleOwner = this
            it.npViewModel = npViewModel
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        playerControlsPanelBinding = null
        npBinding = null
    }

    override fun onSubscribe() {
        goViewModel.subscribeActivePodcast()
        onBackPressed()
    }

    override fun onUnsubscribe() {
        goViewModel.unsubscribeActivePodcast()
        onBackPressed()
    }

    private fun scheduleJobs() {
        val constraints: Constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresCharging(true)
        }.build()

        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TAG_EPISODE_UPDATE_JOB,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun quickMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).apply {
            setGravity(Gravity.CENTER, 0, 0)
            show()
        }
    }

    private fun quickMessage(resId: Int) {
        val string = getString(resId)
        quickMessage(string)
    }

    private fun setupSeekBarProgressListener() {
        npBinding?.npSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                draggingScrubber = false
                playerConnection.seekTo(seekBar.progress.toLong())
            }
        })
    }

    override fun onCloseActivity() {
        finishAndRemoveTask()
    }

    override fun onAppearanceChanged(isThemeChanged: Boolean) {
        if (isThemeChanged) {
            AppCompatDelegate.setDefaultNightMode(
                ThemeUtils.getDefaultNightMode(this)
            )
        } else {
            ThemeUtils.applyChanges(this)
        }
    }

    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            if (isUpdateAvailable && isUpdateAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    appUpdateResultLauncher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                )
            }
        }
    }

    companion object {
        private const val TAG_EPISODE_UPDATE_JOB = "com.colisa.gopods.episodes"
    }
}
