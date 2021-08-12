package com.colisa.podplay.ui.viewmodels

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.network.Result
import com.colisa.podplay.network.models.PodcastResponse
import com.colisa.podplay.repository.RealItunesRepo
import com.colisa.podplay.util.DateUtils
import com.colisa.podplay.util.Event
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel(
    private val itunesRepo: RealItunesRepo
) : ViewModel() {
    private var query: String? = null

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _snackbarText = MutableLiveData<Event<String>>()
    val snackbarText: LiveData<Event<String>> = _snackbarText

    private var _podcasts = MutableLiveData<List<PodcastSummary>>()
    var podcasts: LiveData<List<PodcastSummary>> = _podcasts

    private var _openPodcastDetailEvent = MutableLiveData<Event<PodcastSummary>>()
    var openPodcastDetailEvent: LiveData<Event<PodcastSummary>> = _openPodcastDetailEvent

    fun searchPodcasts(term: String) {
        if (query == term || dataLoading.value == true) {
            return
        } else {
            viewModelScope.launch {
                itunesRepo.searchPodcasts(term)
                    .collect {
                        when (it) {
                            is Result.Loading -> {
                                _dataLoading.value = true
                            }
                            is Result.Success -> {
                                _dataLoading.value = false
                                handleSuccessPodcastSearch(it)
                            }
                            is Result.Error -> {
                                _dataLoading.value = false
                                _snackbarText.value = Event(it.message)
                            }
                        }
                    }
            }
        }

    }

    private fun handleSuccessPodcastSearch(response: Result.Success<PodcastResponse?>) {
        viewModelScope.launch {
            response.data?.results?.let {
                _podcasts.value = it.map { itunesPodcast ->
                    itunesPodcastToPodcastSummary(itunesPodcast)
                }
            }
        }
    }

    fun openPodcast(podcast: PodcastSummary) {
        _openPodcastDetailEvent.value = Event(podcast)
    }


    private fun itunesPodcastToPodcastSummary(i: PodcastResponse.ItunesPodcast): PodcastSummary {
        return PodcastSummary(
            i.collectionName,
            DateUtils.jsonDateToShortDate(i.releaseDate),
            i.artworkUrl100,
            i.feedUrl
        )
    }

    @Parcelize
    data class PodcastSummary(
        var name: String? = "",
        var lastUpdated: String = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    ) : Parcelable

}