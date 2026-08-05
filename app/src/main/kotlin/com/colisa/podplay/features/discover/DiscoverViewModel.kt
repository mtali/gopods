package com.colisa.podplay.features.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.core.common.Result
import com.colisa.podplay.core.data.repository.ItunesRepository
import com.colisa.podplay.core.models.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface DiscoverUiState {
  data object Idle : DiscoverUiState
  data object Loading : DiscoverUiState
  data object NoResults : DiscoverUiState
  data class Success(val podcasts: List<Podcast>) : DiscoverUiState
  data class Error(val message: String?) : DiscoverUiState
}

/** attempt lets the same term be searched again for a retry. */
private data class SearchRequest(val term: String, val attempt: Int = 0)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
  private val itunesRepository: ItunesRepository,
) : ViewModel() {

  // Text field state is not a flow, so typing does not go through the state machine.
  var query by mutableStateOf("")
    private set

  private val request = MutableStateFlow(SearchRequest(term = ""))

  val uiState: StateFlow<DiscoverUiState> = request
    .flatMapLatest { search ->
      if (search.term.isBlank()) {
        flowOf(DiscoverUiState.Idle)
      } else {
        itunesRepository.searchPodcasts(search.term).map { it.asUiState() }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = DiscoverUiState.Idle,
    )

  fun onQueryChange(value: String) {
    query = value
  }

  fun onSearch() {
    val term = query.trim()
    if (term.isEmpty()) return
    request.value = SearchRequest(term, request.value.attempt + 1)
  }

  fun onClearQuery() {
    query = ""
    request.value = SearchRequest(term = "")
  }

  fun onRetry() {
    request.value = request.value.copy(attempt = request.value.attempt + 1)
  }

  /** Cached results are shown while refreshing and after a failed refresh. */
  private fun Result<List<Podcast>>.asUiState(): DiscoverUiState {
    val cached = data
    return when (this) {
      is Result.Loading ->
        if (cached.isNullOrEmpty()) DiscoverUiState.Loading else DiscoverUiState.Success(cached)

      is Result.Success ->
        if (data.isEmpty()) DiscoverUiState.NoResults else DiscoverUiState.Success(data)

      is Result.Error ->
        if (cached.isNullOrEmpty()) {
          DiscoverUiState.Error(exception?.message)
        } else {
          DiscoverUiState.Success(cached)
        }
    }
  }
}
