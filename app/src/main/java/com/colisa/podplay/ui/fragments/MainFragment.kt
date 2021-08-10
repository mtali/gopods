package com.colisa.podplay.ui.fragments

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.colisa.podplay.R
import com.colisa.podplay.databinding.FragmentMainBinding
import timber.log.Timber

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_main_menu, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem.actionView as SearchView
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
                Timber.d("Search pressed!")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}