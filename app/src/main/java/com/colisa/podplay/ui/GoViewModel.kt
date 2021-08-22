package com.colisa.podplay.ui

import android.app.Application
import androidx.lifecycle.*
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

/**
 * This is a main view model for this application
 */
class GoViewModel(application: Application) : AndroidViewModel(application) {

    /* ------------------ UNIVERSAL -------------------*/
    // Loading indicator
    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    /* ------------------ PODCASTS -------------------*/
    // Repos
    // TODO: Later inject
    private val itunesRepo: ItunesRepo by lazy { RealItunesRepo(ItunesService.instance) }
    private val podcastsRepo: PodcastRepo by lazy { PodcastRepo(FeedService.instance) }

    // Search history
    private var query: String? = null

    // Snackbar events
    private val _snackbarEvent = MutableLiveData<Event<String>>()
    val snackbarEvent: LiveData<Event<String>> = _snackbarEvent

    // Search content
    private val _itunesDPodcasts = MutableLiveData<List<DItunesPodcast>>()
    val itunesDPodcasts: LiveData<List<DItunesPodcast>> = _itunesDPodcasts

    // Navigate events
    private val _openPodcastDetails = MutableLiveData<Event<DItunesPodcast>>()
    val openPodcastDetails: LiveData<Event<DItunesPodcast>> = _openPodcastDetails

    val emptyPodcasts: LiveData<Boolean> = Transformations.map(_itunesDPodcasts) {
        it.isEmpty()
    }

    // Label when no podcasts to show
    private val _noPodcastsLabel = MutableLiveData<Int>()
    val noPodcastsLabel: LiveData<Int> = _noPodcastsLabel

    // Icon when no podcasts to show
    private val _noPodcastsIconRes = MutableLiveData<Int>()
    val noPodcastsIconRes: LiveData<Int> = _noPodcastsIconRes


    /* ------------------ PODCASTS DETAILS -------------------*/
    // Active podcast
    private val _activeDPodcast = MutableLiveData<DItunesPodcast>()
    val activeDPodcast: LiveData<DItunesPodcast> = _activeDPodcast

    private var rssJob: Job? = null

    // Rss podcasts
    private val _rssDPodcasts = MutableLiveData<DRssPodcast?>()
    val rssDPodcasts: LiveData<DRssPodcast?> = _rssDPodcasts

    // Is new podcast
    private var loadNewRss = true

    init {
        if (query == null) {
            _itunesDPodcasts.value = emptyList()
        }
    }


    fun loadPodcastRssFeed() {
        if (_dataLoading.value == true) return
        if (loadNewRss) _rssDPodcasts.value = null
        activeDPodcast.value?.let { dItunesPodcast ->
            dItunesPodcast.feedUrl?.let { feedUrl ->
                rssJob?.cancel()
                rssJob = viewModelScope.launch {
                    podcastsRepo.getFeed(feedUrl)
                        .collect { result ->
                            when (result) {
                                is Result.Loading -> _dataLoading.value = true
                                is Result.Error -> {
                                    _dataLoading.value = false
                                    _snackbarEvent.value = Event(result.message)
                                }
                                is Result.Success -> {
                                    _dataLoading.value = false
                                    handleSuccessRssFeed(result)
                                }
                            }
                        }
                }
            }
        }
        if (activeDPodcast.value == null)
            Timber.w("Failed to load podcast when activeDPodcast not initialized")

    }

    /**
     * Handle success rss feed
     * Remember to remove loading state
     */
    private fun handleSuccessRssFeed(result: Result.Success<Podcast?>) {
        viewModelScope.launch {
            result.data?.let { podcast ->
                withContext(Dispatchers.Default) {
                    podcast.imageUrl = activeDPodcast.value?.imageUrl ?: ""
                    try {
                        _rssDPodcasts.postValue(rssPodcastToDRssPodcast(podcast))
                    } finally {
                        _dataLoading.postValue(false)
                    }
                }
            }
        }

    }

    private fun rssPodcastToDRssPodcast(it: Podcast): DRssPodcast {
        return DRssPodcast(
            false,
            htmlToSpannable(it.feedTitle).toString(),
            it.feedUrl,
            htmlToSpannable(it.feedDescription).toString(),
            it.imageUrl,
            rssEpisodeToDRssEpisode(it.episodes)
        )
    }

    private fun rssEpisodeToDRssEpisode(episodes: List<Episode>): List<DRssEpisode> {
        return episodes.map {
            DRssEpisode(
                it.guid,
                htmlToSpannable(it.title).toString(),
                htmlToSpannable(it.description).toString(),
                it.mediaUrl,
                it.releaseDate,
                it.duration
            )
        }
    }


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
                                handleSuccessItunesResponse(it)
                            }
                            is Result.Error -> {
                                _dataLoading.value = false
                                _snackbarEvent.value = Event(it.message)
                            }
                        }
                    }
            }
        }
    }

    private fun handleSuccessItunesResponse(it: Result.Success<PodcastResponse?>) {
        val data = it.data
        if (data == null || data.results.isEmpty()) {
            _snackbarEvent.value = Event("Empty response")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val results = data.results.map { itunesPodcastToDItunesPodcast(it) }
                _itunesDPodcasts.postValue(results)
            }
        }
    }

    private fun itunesPodcastToDItunesPodcast(it: PodcastResponse.ItunesPodcast): DItunesPodcast {
        return DItunesPodcast(
            it.collectionName,
            DateUtils.jsonDateToShortDate(it.releaseDate),
            it.artworkUrl100,
            it.feedUrl
        )
    }

    fun refreshPodcasts() {
        _dataLoading.value = false
    }

    fun refreshPodcastDetails() {
        _dataLoading.value = false
    }

    fun playEpisode(episode: DRssEpisode) {
        Timber.d("Play episode: ${episode.title}")
    }

    /*
        Called when podcast item is clicked
        via binding
     */
    fun openPodcastDetail(podcast: DItunesPodcast) {
        if (podcast.feedUrl == null) {
            _snackbarEvent.value = Event("Podcast link broken")
        } else {
            _openPodcastDetails.value = Event(podcast)
            loadNewRss = (podcast == _activeDPodcast.value).not()
            _activeDPodcast.value = podcast
        }
    }


    /*
        This data class summarize itunes data for views
        Only important data are extracted
     */
    data class DItunesPodcast(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )


    /*
       This class summarize rss data for views
       Podcast contain many episodes
     */
    data class DRssPodcast(
        var subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<DRssEpisode>
    )

    data class DRssEpisode(
        var guid: String? = "",
        var title: String? = "",
        var description: String? = "",
        var mediaUrl: String? = "",
        var releaseDate: Date? = null,
        var duration: String? = ""
    )

}