package com.colisa.podplay.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.core.data.repository.PodcastRepository
import com.colisa.podplay.core.models.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface LibraryUiState {
  data object Loading : LibraryUiState
  data object Empty : LibraryUiState
  data class Success(val podcasts: List<Podcast>) : LibraryUiState
  data class Error(val message: String?) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
  podcastRepository: PodcastRepository,
) : ViewModel() {

  val uiState: StateFlow<LibraryUiState> =
    podcastRepository.getPodcasts(subscribed = true)
      .map { podcasts ->
        if (podcasts.isEmpty()) LibraryUiState.Empty else LibraryUiState.Success(podcasts)
      }
      .catch { emit(LibraryUiState.Error(it.message)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
      )
}
