package com.colisa.podplay.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.colisa.podplay.databinding.FragmentEpisodePlayerBinding
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory

class EpisodePlayerFragment : Fragment() {
    private lateinit var binding: FragmentEpisodePlayerBinding

    private val podcastsViewModel: PodcastViewModel by activityViewModels { ViewModelFactory() }
    private val args: EpisodePlayerFragmentArgs by navArgs()

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
}