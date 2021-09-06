package com.colisa.podplay.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.media.MediaBrowserServiceCompat
import androidx.work.*
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.colisa.podplay.R
import com.colisa.podplay.databinding.ActivityMainBinding
import com.colisa.podplay.databinding.NowPlayingBinding
import com.colisa.podplay.databinding.PlayerControlsPanelBinding
import com.colisa.podplay.extensions.afterMeasured
import com.colisa.podplay.extensions.isPaused
import com.colisa.podplay.extensions.isPlaying
import com.colisa.podplay.extensions.stateName
import com.colisa.podplay.fragments.OnPodcastDetailsListener
import com.colisa.podplay.player.GoPlayerService
import com.colisa.podplay.util.EventObserver
import com.colisa.podplay.util.ThemeUtils
import com.colisa.podplay.util.VersionUtils
import com.colisa.podplay.workers.EpisodeUpdateWorker
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnPodcastDetailsListener {

    // Binding classed
    private var binding: ActivityMainBinding? = null
    private var playerControlsPanelBinding: PlayerControlsPanelBinding? = null
    private var nowPlayingBinding: NowPlayingBinding? = null

    // View model
    private val goViewModel: GoViewModel by viewModels()
    private val npViewModel: NowPlayingViewModel by viewModels()

    private lateinit var playingDialog: MaterialDialog

    // Playback
    private val mediaBrowser: MediaBrowserCompat
            by lazy {
                MediaBrowserCompat(
                    this,
                    ComponentName(this, GoPlayerService::class.java),
                    MediaBrowserCallback(),
                    null
                )
            }

    private var mediaControllerCallback: MediaControllerCallback? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeUtils.getAccentedTheme().first)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        playerControlsPanelBinding = binding!!.playerControls
        setupBinding()

        if (VersionUtils.isOreoMR1()) {
            edgeToEdge {
                binding!!.root.fit { Edge.Top + Edge.Bottom }
            }
        }

        synchronized(Any()) {
            binding!!.mainView.animate().apply {
                duration = 750
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
                togglePlayPause()
            }
        }
    }


    private fun setObservers() {
        goViewModel.playEpisodeEvent.observe(this, EventObserver {
            onEpisodeSelected(it)
        })
    }


    private fun onEpisodeSelected(episode: GoViewModel.REpisode) {
        val podcast = goViewModel.rPodcastFeed.value
        podcast?.let {
            val npEpisode = NowPlayingViewModel.NowPlayingEpisode.from(episode, podcast)
            val controller = MediaControllerCompat.getMediaController(this)
            if (controller.playbackState != null) {
                if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    startPlaying(npEpisode)
                }
            } else {
                startPlaying(npEpisode)
            }
        }
    }

    /**
     * Play podcast episode, call this when all you want to play
     * We don't check conditions here
     */
    private fun startPlaying(np: NowPlayingViewModel.NowPlayingEpisode) {
        val controller = MediaControllerCompat.getMediaController(this)
        npViewModel.saveRecentEpisode(np)
        controller?.let {
            val bundle = Bundle()
            bundle.apply {
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, np.title)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, np.podcastTitle)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, np.artUrl)
            }
            it.transportControls.playFromUri(Uri.parse(np.mediaUrl), bundle)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!mediaBrowser.isConnected) {
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        val controller = MediaControllerCompat.getMediaController(this)
        if (controller != null) {
            mediaControllerCallback?.let {
                controller.unregisterCallback(it)
            }
        }
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
                    openNowPlaying()
                }
            }
        }
    }

    private fun openNowPlaying() {
        playingDialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            customView(R.layout.now_playing)
            if (VersionUtils.isOreoMR1() && !ThemeUtils.isDeviceLand(resources)) {
                edgeToEdge {
                    view.fit { Edge.Bottom }
                }
            }
            getCustomView().afterMeasured {
                playingDialog.setPeekHeight(height)
            }

            onShow {

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
    }

    override fun onSubscribe() {
        goViewModel.saveActivePodcast()
        onBackPressed()
    }

    override fun onUnsubscribe() {
        goViewModel.deleteActivePodcast()
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
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }


    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val controller = MediaControllerCompat(this, token)
        MediaControllerCompat.setMediaController(this, controller)
        mediaControllerCallback = MediaControllerCallback()
        controller.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPause() {
        val controller = MediaControllerCompat.getMediaController(this)
        if (controller != null) {

            if (controller.playbackState != null) {
                val state = controller.playbackState
                when {
                    state.isPlaying -> controller.transportControls.pause()
                    state.isPaused -> controller.transportControls.play()
                    else -> {
                        npViewModel.recentEpisode.value?.let { startPlaying(it) }
                    }
                }
            } else {
                npViewModel.recentEpisode.value?.let { startPlaying(it) }
            }
        }
    }


    /**
     * This receive callbacks for service connection [MediaBrowserServiceCompat]
     */
    inner class MediaBrowserCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Timber.d("onConnected")
            registerMediaController(mediaBrowser.sessionToken)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Timber.d("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Timber.d("onConnectionFailed")
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Timber.d("onMetadataChanged: ${metadata?.mediaMetadata}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            state?.let {
                npViewModel.setIsPlaying(it.isPlaying)
            }
            Timber.d("onPlaybackStateChanged() = ${state?.stateName}")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            Timber.d("onSessionEvent: $event")
        }
    }


    companion object {
        private const val TAG_EPISODE_UPDATE_JOB = "com.colisa.gopods.episodes"
    }
}
