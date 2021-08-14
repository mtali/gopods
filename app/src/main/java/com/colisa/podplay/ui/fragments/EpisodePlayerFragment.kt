package com.colisa.podplay.ui.fragments

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.colisa.podplay.databinding.FragmentEpisodePlayerBinding
import com.colisa.podplay.extensions.requireMediaController
import com.colisa.podplay.playback.PodplayMediaService
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory
import timber.log.Timber

class EpisodePlayerFragment : Fragment() {
    private lateinit var binding: FragmentEpisodePlayerBinding

    private val podcastsViewModel: PodcastViewModel by activityViewModels { ViewModelFactory() }
    private val args: EpisodePlayerFragmentArgs by navArgs()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMediaBrowser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        binding.viewmodel = podcastsViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        val episode = args.episode as PodcastViewModel.EpisodeOnView
        binding.episodeTitleTextview.text = episode.title
        binding.episodeDescriptionTextView.text = episode.description
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val mediaController = MediaControllerCompat(requireActivity(), token)
        MediaControllerCompat.setMediaController(requireActivity(), mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaControllerCallback?.let {
            mediaController.registerCallback(it)
        }
    }

    private fun initMediaBrowser() {
        mediaBrowser = MediaBrowserCompat(
            requireActivity(),
            ComponentName(requireActivity(), PodplayMediaService::class.java),
            MediaBrowserCallback(),
            null
        )
    }

    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected) {
            if (requireMediaController() == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
        } else {
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        if (requireMediaController() == null) {
            mediaControllerCallback?.let {
                requireMediaController()?.unregisterCallback(it)
            }
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Timber.d("Metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Timber.d("State changed to $state")
        }
    }

    inner class MediaBrowserCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            Timber.d("onConnected")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Timber.d("onConnectionFailed")
            // Fatal error handling
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Timber.d("onConnectionFailed")
            // Disable transport controls
        }
    }
}