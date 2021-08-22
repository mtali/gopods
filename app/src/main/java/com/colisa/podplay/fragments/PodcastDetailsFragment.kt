package com.colisa.podplay.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.adapters.EpisodeListAdapter
import com.colisa.podplay.databinding.FragmentPodcastDetailsBinding
import com.colisa.podplay.extensions.setupSnackbar
import com.colisa.podplay.ui.GoViewModel
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class PodcastDetailsFragment : Fragment() {
    private var binding: FragmentPodcastDetailsBinding? = null
    private val goViewModel: GoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding()
        setupSnackbar()
        setupEpisodesListAdapter()
        lifecycleScope.launchWhenStarted {
            goViewModel.loadPodcastRssFeed()
        }
        setupToolbar()
    }


    private fun setupBinding() {
        binding?.let {
            it.lifecycleOwner = viewLifecycleOwner
            it.goViewModel = goViewModel
        }
    }

    private fun setupToolbar() {
        binding?.detailsToolbar?.run {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
        }
    }

    private fun setupSnackbar() {
        view?.setupSnackbar(viewLifecycleOwner, goViewModel.snackbarEvent, Snackbar.LENGTH_SHORT)
    }

    private fun setupEpisodesListAdapter() {
        binding?.let { binding ->
            val viewModel = binding.goViewModel
            if (viewModel != null) {
                val recycler = binding.episodesRv
                val layoutManager = LinearLayoutManager(context)
                val adapter = EpisodeListAdapter(viewModel)
                val divider = DividerItemDecoration(recycler.context, layoutManager.orientation)
                recycler.apply {
                    this.adapter = adapter
                    this.layoutManager = layoutManager
                    this.addItemDecoration(divider)
                }
            } else {
                Timber.w("GoViewModel not initialized when attempting to set up adapter.")
            }
        }
        if (binding == null)
            Timber.w("Unable to set episodes list adapter -> binding is null!")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}