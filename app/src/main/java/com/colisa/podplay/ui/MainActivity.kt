package com.colisa.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.media.MediaBrowserServiceCompat
import androidx.work.*
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.callbacks.onShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.colisa.podplay.R
import com.colisa.podplay.databinding.ActivityMainBinding
import com.colisa.podplay.databinding.NowPlayingBinding
import com.colisa.podplay.databinding.PlayerControlsPanelBinding
import com.colisa.podplay.extensions.*
import com.colisa.podplay.goPreferences
import com.colisa.podplay.player.GoPlayerService
import com.colisa.podplay.ui.fragments.OnPodcastDetailsListener
import com.colisa.podplay.util.EventObserver
import com.colisa.podplay.util.ThemeUtils
import com.colisa.podplay.util.VersionUtils
import com.colisa.podplay.workers.EpisodeUpdateWorker
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnPodcastDetailsListener, UIControlInterface {

    // Binding classed
    private var binding: ActivityMainBinding? = null
    private var playerControlsPanelBinding: PlayerControlsPanelBinding? = null
    private var npBinding: NowPlayingBinding? = null

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

    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    private var episodeDuration: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    togglePlayPause()
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
        updatePlayState()
        updateControlsFromController()
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
        onMediaController { controller ->
            npViewModel.saveRecentEpisode(np)
            val bundle = Bundle()
            bundle.apply {
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, np.title)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, np.podcastTitle)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, np.artUrl600)
            }
            controller.transportControls.playFromUri(Uri.parse(np.mediaUrl), bundle)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!mediaBrowser.isConnected) {
            mediaBrowser.connect()
        } else {
            onMediaController { controller ->
                mediaControllerCallback?.let {
                    controller.registerCallback(it)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        onMediaController { controller ->
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
                bn.npPlay.setOnClickListener { checkIsPlayer { togglePlayPause() } }
                bn.npFastForward.setOnClickListener { checkIsPlayer { fastSeek(true) } }
                bn.npFastRewind.setOnClickListener { checkIsPlayer { fastSeek(false) } }
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

            onShow {
                updateControlsFromController()
            }

            onDismiss {
                npBinding?.npSeekBar?.setOnSeekBarChangeListener(null)
                progressAnimator?.cancel()
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
        onMediaController { controller ->
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

    private fun updatePlayState() {
        onMediaController {
            val isPlaying = it.playbackState.isPlaying
            Timber.d("Update playback state {'playing': $isPlaying, 'service': ${mediaBrowser.isConnected} }")
            npViewModel.setIsPlaying(isPlaying)
        }
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


    private fun fastSeek(isForward: Boolean) = onMediaController {
        val step = goPreferences.fastSeekingStep * 1000
        val isPrepared = it.playbackState.isPrepared
        if (isPrepared) {
            var newPosition = it.playbackState.position
            if (isForward) {
                newPosition += step
            } else {
                newPosition -= step
            }
            it.transportControls.seekTo(newPosition)
        }
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        npViewModel.setEpisodeDuration(episodeDuration)
    }

    private fun setupSeekBarProgressListener() {

        npBinding!!.npSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                npViewModel.setCurrentTime((progress / 1000).toLong())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                draggingScrubber = false
                onMediaController {
                    if (it.playbackState != null) {
                        it.transportControls.seekTo(seekBar.progress.toLong())
                    } else {
                        seekBar.progress = 0
                    }
                }
            }
        })
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0)
            return
        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    npBinding?.let {
                        it.npSeekBar.progress = animator.animatedValue as Int
                    }
                }
            }
            animator.start()
        }
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val progress = position.toInt()
        npBinding?.let { it.npSeekBar.progress = progress }
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            animateScrubber(progress, speed)
        }
    }

    private fun updateControlsFromController() = onMediaController { controller ->
        val metadata = controller.metadata
        if (metadata != null) {
            val state = controller.playbackState
            handleStateChange(state.state, state.position, state.playbackSpeed)
            updateControlsFromMetadata(metadata)
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
            updateControlsFromController()
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
            metadata?.let {
                updateControlsFromMetadata(it)
            }
            Timber.d("onMetadataChanged: ${metadata?.mediaMetadata}")
        }

        override fun onPlaybackStateChanged(playState: PlaybackStateCompat?) {
            val state = playState ?: return
            npViewModel.setPlayState(state.state)
            handleStateChange(state.state, state.position, state.playbackSpeed)
            updatePlayState()
            when {
                state.isError -> quickMessage("Error playing episode!")
            }
            Timber.d("onPlaybackStateChanged() = ${state.stateName}")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            Timber.d("onSessionEvent: $event")
        }
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


    companion object {
        private const val TAG_EPISODE_UPDATE_JOB = "com.colisa.gopods.episodes"
    }
}
