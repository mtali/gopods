package com.colisa.podplay.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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
import com.colisa.podplay.fragments.OnPodcastDetailsListener
import com.colisa.podplay.util.ThemeUtils
import com.colisa.podplay.util.VersionUtils
import com.colisa.podplay.workers.EpisodeUpdateWorker
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnPodcastDetailsListener {

    // Binding classed
    private var binding: ActivityMainBinding? = null
    private var playerControlsPanelBinding: PlayerControlsPanelBinding? = null
    private var nowPlayingBinding: NowPlayingBinding? = null

    // View model
    private val goViewModel: GoViewModel by viewModels()

    private lateinit var playingDialog: MaterialDialog

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
            with(playerBinding.playingSongContainer) {
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


    companion object {
        private const val TAG_EPISODE_UPDATE_JOB = "com.colisa.gopods.episodes"
    }

}