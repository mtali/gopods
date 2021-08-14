package com.colisa.podplay.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.databinding.FragmentPodcastDetailsBinding
import com.colisa.podplay.extensions.setupSnackbar
import com.colisa.podplay.ui.adapters.EpisodeListAdapter
import com.colisa.podplay.ui.viewmodels.MainViewModel
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory
import com.colisa.podplay.util.EventObserver
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class PodcastDetailFragment : Fragment() {
    private lateinit var binding: FragmentPodcastDetailsBinding
    private val args: PodcastDetailFragmentArgs by navArgs()
    private val podcastsViewModel: PodcastViewModel by activityViewModels { ViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = podcastsViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSnackbar()
        loadPodcast()
        setupListAdapter()
        setupNavigation()
    }

    private fun loadPodcast() {
        val podcast = args.podcast as MainViewModel.PodcastSummary
        podcastsViewModel.setCurrentPodcast(podcast)
    }


    private fun setupSnackbar() {
        view?.setupSnackbar(
            viewLifecycleOwner,
            podcastsViewModel.snackbarText,
            Snackbar.LENGTH_SHORT
        )
    }

    private fun setupListAdapter() {
        val viewModel = binding.viewmodel
        if (viewModel != null) {
            val recycler = binding.episodesRecyclerView
            val layoutManager = LinearLayoutManager(context)
            val adapter = EpisodeListAdapter(viewModel)
            val divider = DividerItemDecoration(recycler.context, layoutManager.orientation)
            recycler.apply {
                this.adapter = adapter
                this.layoutManager = layoutManager
                this.addItemDecoration(divider)
            }
        } else {
            Timber.w("ViewModel[PodcastDetailViewModel] not initialized when attempting to set up adapter.")
        }
    }

    private fun setupNavigation() {
        podcastsViewModel.openEpisodeEvent.observe(viewLifecycleOwner, EventObserver {
            openEpisodePlayerFragment(it)
        })
    }

    private fun openEpisodePlayerFragment(episode: PodcastViewModel.EpisodeOnView) {
        val action =
            PodcastDetailFragmentDirections.actionPodcastDetailFragmentToEpisodePlayerFragment(
                episode
            )
        findNavController().navigate(action)
    }

}