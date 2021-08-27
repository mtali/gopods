package com.colisa.podplay.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.R
import com.colisa.podplay.adapters.PodcastsListAdapter
import com.colisa.podplay.databinding.FragmentPodcastsBinding
import com.colisa.podplay.extensions.hideKeyboard
import com.colisa.podplay.extensions.setupSnackbar
import com.colisa.podplay.ui.GoViewModel
import com.colisa.podplay.util.EventObserver
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class PodcastsFragment : Fragment(R.layout.fragment_podcasts), SearchView.OnQueryTextListener {

    private var binding: FragmentPodcastsBinding? = null
    private val goViewModel: GoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPodcastsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding()
        finishSetup()
        setupSnackbar()
        setupListAdapter()
        setupNavigation()
    }

    private fun setupBinding() {
        binding?.let {
            it.lifecycleOwner = viewLifecycleOwner
            it.goViewModel = goViewModel
        }
    }

    private fun finishSetup() {
        binding?.searchToolbar?.let { stb ->
            stb.title = getString(R.string.podcasts)
            stb.inflateMenu(R.menu.menu_search)
            val searchView = stb.menu.findItem(R.id.action_main_search).actionView as SearchView
            searchView.setOnQueryTextListener(this)
            searchView.setOnQueryTextFocusChangeListener { x, hasFocus ->
                Timber.d("setOnQueryTextFocusChangeListener($x, $hasFocus)")
            }
        }
    }

    private fun setupSnackbar() {
        view?.setupSnackbar(viewLifecycleOwner, goViewModel.snackbar, Snackbar.LENGTH_SHORT)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let {
            goViewModel.onSearchPodcast(query)
            view?.hideKeyboard()
        }
        return true
    }

    private fun setupNavigation() {
        goViewModel.openPodcastDetails.observe(viewLifecycleOwner, EventObserver {
            navigateToPodcastDetails(it)
        })
    }

    private fun navigateToPodcastDetails(IPodcast: GoViewModel.IPodcast) {
        val action = PodcastsFragmentDirections.actionPodcastsFragmentToPodcastDetailsFragment()
        findNavController().navigate(action)
    }

    private fun setupListAdapter() {
        val vm = binding?.goViewModel
        if (vm != null) {
            val recycler = binding!!.podcastsRecyclerView
            recycler.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(context)
            val adapter = PodcastsListAdapter(vm)
            val divider = DividerItemDecoration(recycler.context, layoutManager.orientation)
            recycler.apply {
                this.adapter = adapter
                this.layoutManager = layoutManager
                this.addItemDecoration(divider)
            }
        } else {
            Timber.w("GoViewModel not initialized on 'binding' when attempting to set up adapter.")
        }
    }

    override fun onQueryTextChange(newText: String?) = false
}