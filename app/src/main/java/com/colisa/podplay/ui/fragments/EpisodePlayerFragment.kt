package com.colisa.podplay.ui.fragments

import android.content.ComponentName
import android.net.Uri
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.colisa.podplay.databinding.FragmentEpisodePlayerBinding
import com.colisa.podplay.extensions.requireMediaController
import com.colisa.podplay.playback.PodplayMediaService
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.PodcastViewModel.EpisodeOnView
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory
import com.colisa.podplay.util.EventObserver
import kotlinx.coroutines.launch
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewmodel = podcastsViewModel
        }
        setEventsObservers()
    }


    private fun setEventsObservers() {
        podcastsViewModel.playOrPauseEpisodeEvent.observe(viewLifecycleOwner, EventObserver {
            onClickEpisodePlayOrPause(it)
        })
    }

    private fun startPlaying(episode: EpisodeOnView) {
        lifecycleScope.launch {
            val controller = requireMediaController()!!
            val viewData = podcastsViewModel.podcast.value
            viewData?.let { _viewData ->
                val bundle = Bundle()
                bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episode.title)
                bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, _viewData.feedTitle)
                bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, _viewData.imageUrl)
                podcastsViewModel.setPlayState(PlaybackStateCompat.STATE_PLAYING)
                controller.transportControls.playFromUri(Uri.parse(episode.mediaUrl), bundle)
            }
        }
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        Timber.d("MediaController callback registered")
        val controller = MediaControllerCompat(requireActivity(), token)
        MediaControllerCompat.setMediaController(
            requireActivity(),
            controller
        )
        mediaControllerCallback = MediaControllerCallback()
        controller.registerCallback(mediaControllerCallback!!)
    }

    private fun initMediaBrowser() {
        mediaBrowser = MediaBrowserCompat(
            requireActivity(),
            ComponentName(requireActivity(), PodplayMediaService::class.java),
            MediaBrowserCallback(),
            null
        )
    }

    private fun onClickEpisodePlayOrPause(episode: EpisodeOnView) {
        val controller = requireMediaController()!!
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                podcastsViewModel.setPlayState(PlaybackStateCompat.STATE_PAUSED)
                controller.transportControls.pause()
            } else {
                startPlaying(episode)
            }
        } else {
            startPlaying(episode)
        }
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
        if (requireMediaController() != null) {
            mediaControllerCallback?.let {
                Timber.d("MediaController callback unregistered")
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
            state?.let {
                Timber.d("Change play state")
                podcastsViewModel.setPlayState(it.state)
            }

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