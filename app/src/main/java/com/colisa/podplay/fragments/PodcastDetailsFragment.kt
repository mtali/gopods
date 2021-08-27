package com.colisa.podplay.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.R
import com.colisa.podplay.adapters.EpisodeListAdapter
import com.colisa.podplay.databinding.FragmentPodcastDetailsBinding
import com.colisa.podplay.extensions.setupSnackbar
import com.colisa.podplay.ui.GoViewModel
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class PodcastDetailsFragment : Fragment() {
    private var binding: FragmentPodcastDetailsBinding? = null
    private val goViewModel: GoViewModel by activityViewModels()
    private var listener: OnPodcastDetailsListener? = null

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
        finishSetup()
        setupSnackbar()
        setupEpisodesListAdapter()
        lifecycleScope.launchWhenStarted {
            goViewModel.onLoadPodcastRssFeed()
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
        view?.setupSnackbar(viewLifecycleOwner, goViewModel.snackbar, Snackbar.LENGTH_SHORT)
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

    private fun finishSetup() {
        binding?.detailsToolbar?.let { dtb ->
            dtb.inflateMenu(R.menu.details_menu)
            dtb.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_subscribe -> {
                        goViewModel.activeIPodcast.value?.feedUrl?.let {
                            listener?.onSubscribe()
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw IllegalStateException("${requireContext()} must implement OnPodcastDetailsListener")
        }
    }


}

interface OnPodcastDetailsListener {
    fun onSubscribe()
}