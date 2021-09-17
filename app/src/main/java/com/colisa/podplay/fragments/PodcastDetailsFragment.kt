package com.colisa.podplay.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.GoConstants
import com.colisa.podplay.R
import com.colisa.podplay.adapters.EpisodeListAdapter
import com.colisa.podplay.databinding.FragmentPodcastDetailsBinding
import com.colisa.podplay.ui.GoViewModel
import timber.log.Timber

class PodcastDetailsFragment : Fragment() {
    private var binding: FragmentPodcastDetailsBinding? = null
    private val goViewModel: GoViewModel by activityViewModels()
    private var listener: OnPodcastDetailsListener? = null
    private var menuItem: MenuItem? = null

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
        setupEpisodesListAdapter()
        setObservers()
        lifecycleScope.launchWhenStarted {
            goViewModel.onLoadPodcastRssFeed()
            updateSubscribeMenu()
        }
        setupToolbar()
    }

    private fun setObservers() {
        goViewModel.rPodcastFeed.observe(viewLifecycleOwner) { rPodcast ->
            if (rPodcast != null) {
                updateSubscribeMenu()
            }
        }
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
            menuItem = dtb.menu.findItem(R.id.action_subscribe_or_unsubscribe)
            menuItem?.isVisible = false
            dtb.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_subscribe_or_unsubscribe -> {
                        if (goViewModel.rPodcastFeed.value?.subscribed == true) {
                            listener?.onUnsubscribe()
                        } else {
                            listener?.onSubscribe()
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun updateSubscribeMenu() {
        val podcast = goViewModel.rPodcastFeed.value ?: return
        menuItem?.let { item ->
            // Title
            item.title = if (podcast.subscribed) {
                getString(R.string.unsubscribe)
            } else {
                getString(R.string.subscribe)
            }
            // Icon
            val icon = if (podcast.subscribed) {
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_bookmark)
            } else {
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_bookmark_border)
            }

            item.icon = icon

        }
        menuItem?.isVisible = true
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw IllegalStateException("${requireContext()} must implement OnPodcastDetailsListener")
        }

        requireActivity().onBackPressedDispatcher.addCallback {
            goViewModel.onNavigation(GoConstants.DETAILS_FRAGMENT_TAG)
            remove()
            activity?.onBackPressed()
        }
    }
}

interface OnPodcastDetailsListener {
    fun onSubscribe()
    fun onUnsubscribe()
}