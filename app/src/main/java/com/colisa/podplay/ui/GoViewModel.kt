package com.colisa.podplay.ui

import android.app.Application
import androidx.annotation.AnyThread
import androidx.lifecycle.*
import com.colisa.podplay.GoConstants
import com.colisa.podplay.db.GoDatabase
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.network.Resource
import com.colisa.podplay.network.api.FeedService
import com.colisa.podplay.network.api.ItunesService
import com.colisa.podplay.repository.ItunesRepo
import com.colisa.podplay.repository.PodcastRepo
import com.colisa.podplay.repository.RealItunesRepo
import com.colisa.podplay.util.DateUtils
import com.colisa.podplay.util.Event
import com.colisa.podplay.util.HtmlUtils.htmlToSpannable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val db = GoDatabase.getInstance(application)
    private val itunesRepo: ItunesRepo by lazy {
        RealItunesRepo(
            ItunesService.instance,
            db.podcastDao(),
            db
        )
    }
    private val podcastsRepo: PodcastRepo by lazy {
        PodcastRepo(
            FeedService.instance,
            db
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

    // Current IPodcast
    private var _activeIPodcast = MutableLiveData<IPodcast?>()
    var activeIPodcast: LiveData<IPodcast?> = _activeIPodcast

    // Feed
    private var feedJob: Job? = null
    private val _rPodcastFeed = MutableLiveData<RPodcast?>()
    val rPodcastFeed: LiveData<RPodcast?> = _rPodcastFeed

    // Subscribed podcasts
    private var _subscribedIPodcast =
        podcastsRepo.getPodcasts(subscribed = true).map {
            it.toIPodcasts()
        }

    // Podcast
    private var activePodcast: Podcast? = null

    // Live + Subscribed
    private val state = MutableStateFlow(DisplayState.SUBSCRIBED)
    val podcasts: LiveData<List<IPodcast>> = state.flatMapLatest { state ->
        if (state == DisplayState.LIVE) {
            _searchPodcasts.asFlow()
        } else {
            _subscribedIPodcast
        }
    }.asLiveData()

    // Episode
    private val _playEpisodeEvent = MutableLiveData<Event<REpisode>>()
    val playEpisodeEvent: LiveData<Event<REpisode>> = _playEpisodeEvent

    // No subscribed podcast
    val noSubscribedPodcasts = podcasts.map {
        it.isNullOrEmpty()
    }


    private fun showLive() {
        state.value = DisplayState.LIVE
    }

    fun showSubscribed() {
        state.value = DisplayState.SUBSCRIBED
    }

    private fun spinner(state: Boolean) {
        _spinner.value = state
    }

    private fun message(msg: String?) {
        _snackbar.value = Event(msg ?: "Unexpected error")
    }

    fun onSearchPodcast(term: String) {
        // Prevent user perform same search while work is still on progress
        // if (term == query && searchJob?.isActive == true) return
        query = term

        showLive()

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            itunesRepo.searchPodcasts(term)
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            if (!result.data.isNullOrEmpty()) {
                                _searchPodcasts.value = result.data.toIPodcasts()
                                spinner(false)
                            } else {
                                spinner(true)
                            }
                        }

                        is Resource.Error -> {
                            spinner(false)
                            message(result.error?.message)
                        }

                        is Resource.Success -> {
                            if (result.data.isNullOrEmpty()) {
                                message("Empty response")
                            } else {
                                _searchPodcasts.value = result.data.toIPodcasts()
                            }
                            spinner(false)
                        }
                    }
                }

        }
    }

    private fun fetchPodcastFeed(url: String, block: suspend (Podcast) -> Unit) {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            podcastsRepo.getPodcastFeed(url)
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            result.data?.let { podcast ->
                                block(podcast)
                                spinner
                            }
                            // If there are episodes we don't show loading
                            spinner(result.data?.episodes.isNullOrEmpty())
                        }

                        is Resource.Error -> {
                            spinner(false)
                            message(result.error?.message)
                        }

                        is Resource.Success -> {
                            val podcast = result.data
                            podcast?.let {
                                block(it)
                            }
                            spinner(false)
                        }
                    }
                }
        }
    }

    fun onLoadPodcastRssFeed() {
        val url = _activeIPodcast.value?.feedUrl ?: return

        fetchPodcastFeed(url) {
            activePodcast = it
            _rPodcastFeed.value = it.toRPodcastMainSafe()
        }
    }

    fun onNavigation(from: String) {
        when (from) {
            GoConstants.DETAILS_FRAGMENT_TAG -> {
                spinner(false)
                feedJob?.cancel()
                // TODO: Clean other variables
            }
            else -> {
                throw IllegalStateException("Unknown back navigation tag: '${from}'")
            }
        }
    }

    fun setActivePodcast(feedUrl: String) {
        fetchPodcastFeed(url = feedUrl) { podcast ->
            openPodcastDetail(podcast.toIPodcast())
        }
    }

    fun subscribeActivePodcast() {
        viewModelScope.launch {
            activePodcast?.let { podcast ->
                podcastsRepo.subscribePodcast(podcast, true)
            }
        }
    }

    fun unsubscribeActivePodcast() {
        viewModelScope.launch {
            activePodcast?.let { podcast ->
                podcastsRepo.subscribePodcast(podcast, false)
            }
        }
    }


    @AnyThread
    suspend fun Podcast.toRPodcastMainSafe() = withContext(Dispatchers.Default) {
        toRPodcast()
    }

    private fun Podcast.toRPodcast(): RPodcast {
        return RPodcast(
            subscribed = subscribed,
            feedTitle = htmlToSpannable(feedTitle).toString(),
            feedUrl = feedUrl,
            feedDesc = htmlToSpannable(feedDescription).toString(),
            imageUrl = imageUrl,
            imageUrl600 = imageUrl600,
            episodes = episodes.toREpisodes()
        )
    }

    @AnyThread
    private suspend fun List<Podcast>.toIPodcasts(): List<IPodcast> = withContext(Dispatchers.IO) {
        map { it.toIPodcast() }
    }

    private fun Podcast.toIPodcast(): IPodcast {
        return IPodcast(
            name = feedTitle,
            lastUpdated = DateUtils.dateToShortDate(lastUpdated),
            imageUrl = imageUrl,
            imageUrl600 = imageUrl600,
            feedUrl = feedUrl
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
        _playEpisodeEvent.value = Event(episode)
    }

    /*
        Called when podcast item is clicked
        via binding
     */
    fun openPodcastDetail(podcast: IPodcast) {
        if (podcast.feedUrl == null) {
            _snackbar.value = Event("Podcast link broken")
        } else {
            _rPodcastFeed.value = null
            _activeIPodcast.value = podcast
            _openPodcastDetails.value = Event(podcast)

        }
    }

    data class IPodcast(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var imageUrl600: String? = "",
        var feedUrl: String? = ""
    )

    data class RPodcast(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var imageUrl600: String? = "",
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