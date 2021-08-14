package com.colisa.podplay.ui.fragments

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.colisa.podplay.R
import com.colisa.podplay.databinding.FragmentMainBinding
import com.colisa.podplay.extensions.hideKeyboard
import com.colisa.podplay.extensions.setupSnackbar
import com.colisa.podplay.ui.adapters.PodcastListAdapter
import com.colisa.podplay.ui.viewmodels.MainViewModel
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import com.colisa.podplay.ui.viewmodels.factory.ViewModelFactory
import com.colisa.podplay.util.EventObserver
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private lateinit var searchView: SearchView
    private val podcastsViewModel: PodcastViewModel by activityViewModels { ViewModelFactory() }
    private val mainViewModel: MainViewModel by viewModels { ViewModelFactory() }
    private lateinit var podcastAdapter: PodcastListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        podcastsViewModel.cleanPodcastData()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_main_menu, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem.actionView as SearchView
        setupSearchView()
        searchView.queryHint = getString(R.string.search)
        activity?.let {
            val searchManager = it.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.setSearchableInfo(
                searchManager.getSearchableInfo(it.componentName)
            )
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = mainViewModel
        setupSnackbar()
        setupNavigation()
        setupListAdapter()

    }

    private fun setupListAdapter() {
        val viewModel = binding.viewmodel
        if (viewModel != null) {
            val recycler = binding.podcastRecyclerView
            recycler.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(context)
            val adapter = PodcastListAdapter(viewModel)
            val divider = DividerItemDecoration(recycler.context, layoutManager.orientation)
            recycler.apply {
                this.adapter = adapter
                this.layoutManager = layoutManager
                this.addItemDecoration(divider)
            }
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    private fun setupSnackbar() {
        view?.setupSnackbar(viewLifecycleOwner, mainViewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }

    private fun performSearch(term: String) {
        mainViewModel.searchPodcasts(term)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    performSearch(it)
                    view?.hideKeyboard()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun setupNavigation() {
        mainViewModel.openPodcastDetailEvent.observe(viewLifecycleOwner, EventObserver {
            openPodcastDetails(it)
        })
    }

    private fun openPodcastDetails(podcast: MainViewModel.PodcastSummary) {
        val action = MainFragmentDirections.actionMainFragmentToPodcastDetailFragment(podcast)
        findNavController().navigate(action)
    }

}