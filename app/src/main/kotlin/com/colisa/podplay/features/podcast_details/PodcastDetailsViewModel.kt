package com.colisa.podplay.features.podcast_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.common.utils.DateUtils
import com.colisa.podplay.core.common.utils.HtmlUtils.htmlToText
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.dispatchers.Dispatcher
import com.colisa.podplay.core.dispatchers.GoDispatchers.Default
import com.colisa.podplay.core.models.NowPlayingEpisode
import com.colisa.podplay.core.models.Podcast
import com.colisa.podplay.core.player.PlayerConnection
import com.colisa.podplay.features.podcast_details.navigation.PodcastDetailsNavKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class EpisodeUi(
  val guid: String,
  val title: String,
  val description: String,
  val mediaUrl: String,
  val releaseDate: String,
  val duration: String,
)

data class PodcastDetailsUi(
  val feedUrl: String,
  val title: String,
  val description: String,
  val imageUrl: String,
  val imageUrlLarge: String,
  val subscribed: Boolean,
  val episodes: List<EpisodeUi>,
)

sealed interface PodcastDetailsUiState {
  data object Loading : PodcastDetailsUiState
  data class Success(val podcast: PodcastDetailsUi) : PodcastDetailsUiState
  data class Error(val message: String?) : PodcastDetailsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PodcastDetailsViewModel.Factory::class)
class PodcastDetailsViewModel @AssistedInject constructor(
  private val podcastRepository: PodcastRepository,
  private val playerConnection: PlayerConnection,
  @param:Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
  @Assisted private val navKey: PodcastDetailsNavKey,
) : ViewModel() {

  private val refreshTrigger = MutableStateFlow(0)

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  /** Kept so an episode can be paired with its podcast when playback starts. */
  private var currentPodcast: PodcastDetailsUi? = null

  val uiState: StateFlow<PodcastDetailsUiState> = refreshTrigger
    .flatMapLatest { podcastRepository.getPodcastFeed(navKey.feedUrl) }
    .map { result -> result.asUiState() }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = PodcastDetailsUiState.Loading,
    )

  fun onRefresh() {
    _isRefreshing.value = true
    refreshTrigger.value += 1
  }

  fun onToggleSubscribe() {
    val podcast = currentPodcast ?: return
    viewModelScope.launch {
      val stored = podcastRepository.getPodcast(navKey.feedUrl) ?: return@launch
      podcastRepository.subscribePodcast(stored, !podcast.subscribed)
    }
  }

  fun onPlayEpisode(episode: EpisodeUi) {
    val podcast = currentPodcast ?: return
    if (episode.mediaUrl.isBlank()) return
    playerConnection.play(
      NowPlayingEpisode(
        title = episode.title,
        artUrl = podcast.imageUrl,
        artUrl600 = podcast.imageUrlLarge,
        mediaUrl = episode.mediaUrl,
        description = episode.description,
        podcastTitle = podcast.title,
      )
    )
  }

  private suspend fun Result<Podcast>.asUiState(): PodcastDetailsUiState {
    val cached = data
    return when (this) {
      is Result.Loading ->
        if (cached == null) {
          PodcastDetailsUiState.Loading
        } else {
          PodcastDetailsUiState.Success(cached.asUi())
        }

      is Result.Success -> {
        _isRefreshing.value = false
        PodcastDetailsUiState.Success(data.asUi())
      }

      // Stale episodes beat an error screen, but with nothing cached to show the
      // failure has to surface so the user can retry.
      is Result.Error -> {
        _isRefreshing.value = false
        if (cached == null || cached.episodes.isEmpty()) {
          PodcastDetailsUiState.Error(exception?.message)
        } else {
          PodcastDetailsUiState.Success(cached.asUi())
        }
      }
    }
  }

  /** Feed markup is stripped off the main thread. */
  private suspend fun Podcast.asUi(): PodcastDetailsUi = withContext(defaultDispatcher) {
    PodcastDetailsUi(
      feedUrl = feedUrl,
      title = htmlToText(feedTitle),
      description = htmlToText(feedDescription),
      imageUrl = imageUrl,
      imageUrlLarge = imageUrl600,
      subscribed = subscribed,
      episodes = episodes.map { episode ->
        EpisodeUi(
          guid = episode.guid,
          title = htmlToText(episode.title),
          description = htmlToText(episode.description),
          mediaUrl = episode.mediaUrl,
          releaseDate = DateUtils.dateToShortDate(episode.releaseDate),
          duration = episode.duration,
        )
      },
    ).also { currentPodcast = it }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: PodcastDetailsNavKey): PodcastDetailsViewModel
  }
}
