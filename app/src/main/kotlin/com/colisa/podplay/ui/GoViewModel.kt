package com.colisa.podplay.ui

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.GoConstants
import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.common.utils.HtmlUtils.htmlToText
import com.colisa.podplay.core.data.repository.ItunesRepository
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.Default
import com.colisa.podplay.core.models.Episode
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

enum class DisplayState {
  LIVE,
  SUBSCRIBED
}

@HiltViewModel
class GoViewModel @Inject constructor(
  private val itunesRepository: ItunesRepository,
  private val podcastRepository: PodcastRepository,
  @param:Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

  private val _spinner = MutableLiveData<Boolean>()
  val spinner: LiveData<Boolean> = _spinner

  private val _snackbar = MutableLiveData<Event<String>>()
  val snackbar: LiveData<Event<String>> = _snackbar

  private val _openPodcastDetails = MutableLiveData<Event<IPodcast>>()
  val openPodcastDetails: LiveData<Event<IPodcast>> = _openPodcastDetails

  private var query: String? = null
  private var searchJob: Job? = null
  private val _searchPodcasts = MutableLiveData<List<IPodcast>>()

  private val _activeIPodcast = MutableLiveData<IPodcast?>()
  val activeIPodcast: LiveData<IPodcast?> = _activeIPodcast

  private var feedJob: Job? = null
  private val _rPodcastFeed = MutableLiveData<RPodcast?>()
  val rPodcastFeed: LiveData<RPodcast?> = _rPodcastFeed

  private val subscribedPodcasts = podcastRepository.getPodcasts(subscribed = true)
    .map { podcasts -> podcasts.toIPodcasts() }

  private var activePodcast: Podcast? = null

  private val state = MutableStateFlow(DisplayState.SUBSCRIBED)

  val podcasts: LiveData<List<IPodcast>> = state.flatMapLatest { displayState ->
    if (displayState == DisplayState.LIVE) {
      _searchPodcasts.asFlow()
    } else {
      subscribedPodcasts
    }
  }.asLiveData()

  private val _playEpisodeEvent = MutableLiveData<Event<REpisode>>()
  val playEpisodeEvent: LiveData<Event<REpisode>> = _playEpisodeEvent

  val noSubscribedPodcasts = podcasts.map { it.isEmpty() }

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
    query = term
    showLive()

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      itunesRepository.searchPodcasts(term).collect { result ->
        when (result) {
          is Result.Loading<List<Podcast>> -> {
            val cached = result.data
            if (!cached.isNullOrEmpty()) {
              _searchPodcasts.value = cached.toIPodcasts()
              spinner(false)
            } else {
              spinner(true)
            }
          }

          is Result.Error<List<Podcast>> -> {
            spinner(false)
            message(result.exception?.message)
          }

          is Result.Success<List<Podcast>> -> {
            if (result.data.isEmpty()) {
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
      podcastRepository.getPodcastFeed(url).collect { result ->
        when (result) {
          is Result.Loading<Podcast> -> {
            result.data?.let { block(it) }
            // Loading is only shown when there is nothing cached to display.
            spinner(result.data?.episodes.isNullOrEmpty())
          }

          is Result.Error<Podcast> -> {
            spinner(false)
            message(result.exception?.message)
          }

          is Result.Success<Podcast> -> {
            block(result.data)
            spinner(false)
          }
        }
      }
    }
  }

  fun onLoadPodcastRssFeed() {
    val url = _activeIPodcast.value?.feedUrl ?: return
    fetchPodcastFeed(url) { podcast ->
      activePodcast = podcast
      _rPodcastFeed.value = podcast.toRPodcastMainSafe()
    }
  }

  fun onNavigation(from: String) {
    when (from) {
      GoConstants.DETAILS_FRAGMENT_TAG -> {
        spinner(false)
        feedJob?.cancel()
      }

      else -> throw IllegalStateException("Unknown back navigation tag: '$from'")
    }
  }

  fun setActivePodcast(feedUrl: String) {
    fetchPodcastFeed(url = feedUrl) { podcast ->
      openPodcastDetail(podcast.toIPodcast())
    }
  }

  fun subscribeActivePodcast() {
    viewModelScope.launch {
      activePodcast?.let { podcastRepository.subscribePodcast(it, true) }
    }
  }

  fun unsubscribeActivePodcast() {
    viewModelScope.launch {
      activePodcast?.let { podcastRepository.subscribePodcast(it, false) }
    }
  }

  @AnyThread
  private suspend fun Podcast.toRPodcastMainSafe() = withContext(defaultDispatcher) {
    toRPodcast()
  }

  private fun Podcast.toRPodcast(): RPodcast = RPodcast(
    subscribed = subscribed,
    feedTitle = htmlToText(feedTitle),
    feedUrl = feedUrl,
    feedDesc = htmlToText(feedDescription),
    imageUrl = imageUrl,
    imageUrl600 = imageUrl600,
    episodes = episodes.toREpisodes(),
  )

  @AnyThread
  private suspend fun List<Podcast>.toIPodcasts(): List<IPodcast> =
    withContext(defaultDispatcher) { map { it.toIPodcast() } }

  private fun Podcast.toIPodcast(): IPodcast = IPodcast(
    name = feedTitle,
    lastUpdated = DateUtils.dateToShortDate(lastUpdated),
    imageUrl = imageUrl,
    imageUrl600 = imageUrl600,
    feedUrl = feedUrl,
  )

  private fun List<Episode>.toREpisodes(): List<REpisode> = map {
    REpisode(
      guid = it.guid,
      title = htmlToText(it.title),
      description = htmlToText(it.description),
      mediaUrl = it.mediaUrl,
      releaseDate = it.releaseDate,
      duration = it.duration,
    )
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

  /** Called when a podcast row is tapped, via data binding. */
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
    var feedUrl: String? = "",
  )

  data class RPodcast(
    var subscribed: Boolean = false,
    var feedTitle: String? = "",
    var feedUrl: String? = "",
    var feedDesc: String? = "",
    var imageUrl: String? = "",
    var imageUrl600: String? = "",
    var episodes: List<REpisode>,
  )

  data class REpisode(
    var guid: String? = "",
    var title: String? = "",
    var description: String? = "",
    var mediaUrl: String? = "",
    var releaseDate: Date? = null,
    var duration: String? = "",
  )
}
