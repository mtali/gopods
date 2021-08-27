package com.colisa.podplay.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.colisa.podplay.R
import com.colisa.podplay.databinding.ActivityMainBinding
import com.colisa.podplay.databinding.PlayerControlsPanelBinding
import com.colisa.podplay.fragments.OnPodcastDetailsListener
import com.colisa.podplay.util.ThemeUtils
import com.colisa.podplay.util.VersionUtils
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge
import timber.log.Timber

class MainActivity : AppCompatActivity(), OnPodcastDetailsListener {

    // Binding classed
    private var binding: ActivityMainBinding? = null
    private var playerControlsPanelBinding: PlayerControlsPanelBinding? = null

    // View model
    private val goViewModel: GoViewModel by viewModels()

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
        Timber.d("Open now playing!")
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

}