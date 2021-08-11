package com.colisa.podplay.ui.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.repository.RealItunesRepo
import com.colisa.podplay.ui.viewmodels.MainViewModel
import timber.log.Timber

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Timber.d("Creating MainViewModel")
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val repo = RealItunesRepo(ItunesService.instance)
            return MainViewModel(repo) as T
        }
        throw IllegalStateException("Unknown view model")
    }
}