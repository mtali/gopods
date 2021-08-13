package com.colisa.podplay.ui.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.repository.RealItunesRepo
import com.colisa.podplay.ui.viewmodels.MainViewModel
import com.colisa.podplay.ui.viewmodels.PodcastDetailViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val repo = RealItunesRepo(ItunesService.instance)
            return MainViewModel(repo) as T
        } else if (modelClass.isAssignableFrom(PodcastDetailViewModel::class.java)) {
            val repo = PodcastRepo(FeedService.instance)
            return PodcastDetailViewModel(repo) as T
        }
        throw IllegalStateException("Unknown view model")
    }
}