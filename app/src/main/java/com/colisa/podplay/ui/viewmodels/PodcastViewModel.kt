package com.colisa.podplay.ui.viewmodels

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Result
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.util.Event
import com.colisa.podplay.util.HtmlUtils
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class PodcastViewModel(
    private val repo: PodcastRepo
) : ViewModel() {

    private var podcastSummary: MainViewModel.PodcastSummary? = null

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _snackbarText = MutableLiveData<Event<String>>()
    val snackbarText: LiveData<Event<String>> = _snackbarText

//    private val _activePodcast = MutableLiveData<PodcastOnView>()
//    val activePodcast: LiveData<PodcastOnView> = _activePodcast

    private val _openEpisodeEvent = MutableLiveData<Event<EpisodeOnView>>()
    val openEpisodeEvent: LiveData<Event<EpisodeOnView>> = _openEpisodeEvent

    private val _currentEpisode = MutableLiveData<EpisodeOnView?>()
    val currentEpisode: LiveData<EpisodeOnView?> = _currentEpisode

    private val _podcast = MutableLiveData<PodcastOnView?>()
    val podcast: LiveData<PodcastOnView?> = _podcast

    private val _playOrPauseEpisodeEvent = MutableLiveData<Event<EpisodeOnView>>()
    val playOrPauseEpisodeEvent: LiveData<Event<EpisodeOnView>> = _playOrPauseEpisodeEvent

    private val _playbackState = MutableLiveData<Int>()
    val playbackState: LiveData<Int> = _playbackState

    fun setCurrentPodcast(podcast: MainViewModel.PodcastSummary) {
        if (podcastSummary != null && podcast == podcastSummary) {
            return
        } else {
            podcastSummary = podcast
            loadPodcasts()
        }
    }

    fun setPlayState(state: Int) {
        _playbackState.value = state
    }

    private fun loadPodcasts() {
        if (_dataLoading.value == true) return
        podcastSummary?.let { summary ->
            summary.feedUrl?.let { url ->
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
            return@let
        }

        if (podcastSummary == null)
            Timber.w("Failed to load podcast when podcast summary not initialized")

    }

    fun refresh() {
        loadPodcasts()
    }

    private fun handleSuccessRssFeed(p: Result.Success<Podcast?>) {
        viewModelScope.launch {
            p.data?.let {
                it.imageUrl = podcastSummary?.imageUrl ?: ""
                _podcast.value = podcastToPodcastOnView(it)
            }
        }
    }


    private fun episodeToEpisodeOnView(episodes: List<Episode>): List<EpisodeOnView> {
        return episodes.map {
            EpisodeOnView(
                it.guid,
                HtmlUtils.htmlToSpannable(it.title).toString(),
                HtmlUtils.htmlToSpannable(it.description).toString(),
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }

    private fun podcastToPodcastOnView(it: Podcast): PodcastOnView {
        return PodcastOnView(
            false,
            HtmlUtils.htmlToSpannable(it.feedTitle).toString(),
            it.feedUrl,
            HtmlUtils.htmlToSpannable(it.feedDescription).toString(),
            it.imageUrl,
            episodeToEpisodeOnView(it.episodes)
        )
    }

    fun openEpisode(episode: EpisodeOnView) {
        _openEpisodeEvent.value = Event(episode)
        updateCurrentEpisode(episode)

    }

    private fun updateCurrentEpisode(episode: EpisodeOnView) {
        _currentEpisode.value = episode
    }

    fun playOrPauseEpisode() {
        _currentEpisode.value?.let {
            _playOrPauseEpisodeEvent.value = Event(it)
        }
    }

    data class PodcastOnView(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeOnView>
    )

    @Parcelize
    data class EpisodeOnView(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    ) : Parcelable

    fun cleanPodcastData() {
        viewModelScope.launch {
            podcastSummary = null
            _podcast.value = null
        }
    }
}