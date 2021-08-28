package com.colisa.podplay.ui

import android.app.Application
import androidx.annotation.AnyThread
import androidx.lifecycle.*
import com.colisa.podplay.db.GoDatabase
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Result
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.network.models.PodcastResponse
import com.colisa.podplay.repository.ItunesRepo
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.repository.RealItunesRepo
import com.colisa.podplay.util.DateUtils
import com.colisa.podplay.util.Event
import com.colisa.podplay.util.HtmlUtils.htmlToSpannable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*


enum class DisplayState {
    LIVE,
    SUBSCRIBED
}

/**
 * This is a main view model for this application
 */
class GoViewModel(application: Application) : AndroidViewModel(application) {

    // Spinner
    private val _spinner = MutableLiveData<Boolean>()
    val spinner: LiveData<Boolean> = _spinner

    // Configurations
    private val itunesRepo: ItunesRepo by lazy { RealItunesRepo(ItunesService.instance) }
    private val podcastsRepo: PodcastRepo by lazy {
        PodcastRepo(
            FeedService.instance,
            GoDatabase.getInstance(application).podcastDao()
        )
    }

    // Snackbar
    private val _snackbar = MutableLiveData<Event<String>>()
    val snackbar: LiveData<Event<String>> = _snackbar

    // Navigation
    private val _openPodcastDetails = MutableLiveData<Event<IPodcast>>()
    val openPodcastDetails: LiveData<Event<IPodcast>> = _openPodcastDetails


    // Search
    private var query: String? = null
    private var searchJob: Job? = null
    private val _searchPodcasts = MutableLiveData<List<IPodcast>>()
    val searchPodcasts: LiveData<List<IPodcast>> = _searchPodcasts

    // Current IPodcast
    private var _activeIPodcast = MutableLiveData<IPodcast?>()
    var activeIPodcast: LiveData<IPodcast?> = _activeIPodcast

    // Feed
    private var loadNewFeed = true
    private var feedJob: Job? = null
    private val _rPodcastFeed = MutableLiveData<RPodcast?>()
    val rPodcastFeed: LiveData<RPodcast?> = _rPodcastFeed

    // Subscribed podcasts
    private var _subscribedIPodcast =
        podcastsRepo.getSubscribedPodcasts().switchMap { list ->
            liveData {
                emit(list.toIPodcasts())
            }
        }

    // Podcast
    private var activePodcast: Podcast? = null

    // Live + Subscribed
    private val state = MutableStateFlow(DisplayState.SUBSCRIBED)
    val podcasts: LiveData<List<IPodcast>> = state.flatMapLatest { state ->
        if (state == DisplayState.LIVE) {
            _searchPodcasts.asFlow()
        } else {
            _subscribedIPodcast.asFlow()
        }
    }.asLiveData()

    private fun showLive() {
        state.value = DisplayState.LIVE
    }

    fun showSubscribed() {
        state.value = DisplayState.SUBSCRIBED
    }

    fun onSearchPodcast(term: String) {
        if (term == query && searchJob?.isActive == true) return
        query = term
        showLive()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            itunesRepo.searchPodcasts(term)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _spinner.value = true
                        }
                        is Result.Error -> {
                            _spinner.value = false
                            _snackbar.value = Event(result.exception.message ?: "Unknown error!")
                        }
                        is Result.OK -> {
                            val data = result.data
                            if (data == null || data.resultCount == 0) {
                                _snackbar.value = Event("Empty response")
                            } else {
                                _searchPodcasts.value = data.toIPodcastsMainSafe()
                            }
                            _spinner.value = false
                        }
                    }
                }
        }
    }

    @AnyThread
    suspend fun PodcastResponse.toIPodcastsMainSafe() = withContext(Dispatchers.Default) {
        toIPodcasts()
    }

    private fun PodcastResponse.toIPodcasts(): List<IPodcast> {
        return results.map {
            IPodcast(
                it.collectionName,
                DateUtils.jsonDateToShortDate(it.releaseDate),
                it.artworkUrl100,
                it.feedUrl
            )
        }
    }

    fun onLoadPodcastRssFeed() {
        feedJob?.cancel()
        _activeIPodcast.value?.let { iPodcast ->
            val url = iPodcast.feedUrl ?: return@let
            feedJob = viewModelScope.launch {
                podcastsRepo.getPodcasts(url)
                    .collect { result ->
                        when (result) {
                            is Result.Loading -> {
                                _spinner.value = true
                            }
                            is Result.Error -> {
                                _spinner.value = false
                                _snackbar.value =
                                    Event(result.exception.message ?: "Unknown error!")
                            }
                            is Result.OK -> {
                                val data = result.data
                                if (data == null) {
                                    _snackbar.value = Event("Empty response")
                                } else {
                                    data.imageUrl = activeIPodcast.value?.imageUrl ?: ""
                                    activePodcast = data
                                    _rPodcastFeed.value = data.toRPodcastMainSafe()
                                }
                                _spinner.value = false
                            }
                        }
                    }
            }
        }
    }

    fun saveActivePodcast() {
        activePodcast?.let {
            viewModelScope.launch {
                podcastsRepo.savePodcast(it)
            }
        }
    }

    fun deleteActivePodcast() {
        activePodcast?.let {
            viewModelScope.launch {
                podcastsRepo.deletePodcast(it)
            }
        }
    }


    @AnyThread
    suspend fun Podcast.toRPodcastMainSafe() = withContext(Dispatchers.Default) {
        toRPodcast()
    }

    private fun Podcast.toRPodcast(): RPodcast {
        return RPodcast(
            id != null,
            htmlToSpannable(feedTitle).toString(),
            feedUrl,
            htmlToSpannable(feedDescription).toString(),
            imageUrl,
            episodes.toREpisodes()
        )
    }

    @AnyThread
    private suspend fun List<Podcast>.toIPodcasts(): List<IPodcast> = withContext(Dispatchers.IO) {
        map { it.toIPodcast() }
    }

    private fun Podcast.toIPodcast(): IPodcast {
        return IPodcast(
            feedTitle,
            DateUtils.dateToShortDate(lastUpdated),
            imageUrl,
            feedUrl
        )
    }

    private fun List<Episode>.toREpisodes(): List<REpisode> {
        return map {
            REpisode(
                it.guid,
                htmlToSpannable(it.title).toString(),
                htmlToSpannable(it.description).toString(),
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }


    fun refreshPodcasts() {
        _spinner.value = false
    }

    fun refreshPodcastDetails() {
        _spinner.value = false
    }

    fun playEpisode(episode: REpisode) {
        Timber.d("Play episode: ${episode.title}")
    }

    /*
        Called when podcast item is clicked
        via binding
     */
    fun openPodcastDetail(podcast: IPodcast) {
        if (podcast.feedUrl == null) {
            _snackbar.value = Event("Podcast link broken")
        } else {
            loadNewFeed = _activeIPodcast.value != podcast
            _rPodcastFeed.value = null
            _activeIPodcast.value = podcast
            _openPodcastDetails.value = Event(podcast)

        }
    }

    data class IPodcast(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )

    data class RPodcast(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<REpisode>
    )

    data class REpisode(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    )

}