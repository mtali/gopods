package com.colisa.podplay.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Result
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.util.Event
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class PodcastDetailViewModel(
    private val repo: PodcastRepo
) : ViewModel() {

    private lateinit var podcastSummary: MainViewModel.PodcastSummary

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _snackbarText = MutableLiveData<Event<String>>()
    val snackbarText: LiveData<Event<String>> = _snackbarText

    private val _activePodcast = MutableLiveData<PodcastOnView>()
    val activePodcast: LiveData<PodcastOnView> = _activePodcast


    private val _podcast = MutableLiveData<PodcastOnView>()
    val podcast: LiveData<PodcastOnView> = _podcast

    fun setCurrentPodcast(podcast: MainViewModel.PodcastSummary) {
        podcastSummary = podcast
        loadPodcasts()
    }

    fun loadPodcasts() {
        if (_dataLoading.value == true) return
        if (this::podcastSummary.isInitialized) {
            val feedUrl = podcastSummary.feedUrl
            feedUrl?.let { url ->
                viewModelScope.launch {
                    repo.getFeed(url)
                        .collect {
                            when (it) {
                                is Result.Loading -> _dataLoading.value = true
                                is Result.Error -> {
                                    _dataLoading.value = false
                                    _snackbarText.value = Event(it.message)
                                }
                                is Result.Success -> {
                                    _dataLoading.value = false
                                    handleSuccessRssFeed(it)
                                }
                            }
                        }
                }
            }

        } else {
            Timber.w("Failed to podcast when podcast summary not initialized")
        }
    }

    fun refresh() {
        loadPodcasts()
    }

    private fun handleSuccessRssFeed(p: Result.Success<Podcast?>) {
        viewModelScope.launch {
            p.data?.let {
                it.imageUrl = podcastSummary.imageUrl ?: ""
                _podcast.value = podcastToPodcastOnView(it)
                Timber.d("Loaded: $it")
            }
        }
    }


    private fun episodeToEpisodeOnView(episodes: List<Episode>): List<EpisodeOnView> {
        return episodes.map {
            EpisodeOnView(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }

    private fun podcastToPodcastOnView(it: Podcast): PodcastOnView {
        return PodcastOnView(
            false,
            it.feedTitle,
            it.feedUrl,
            it.feedDescription,
            it.imageUrl,
            episodeToEpisodeOnView(it.episodes)
        )
    }

    data class PodcastOnView(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeOnView>
    )

    data class EpisodeOnView(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    )
}