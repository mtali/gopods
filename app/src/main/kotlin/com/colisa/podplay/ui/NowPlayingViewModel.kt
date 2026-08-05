package com.colisa.podplay.ui

import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.app.goPreferences
import com.squareup.moshi.JsonClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor() : ViewModel() {

  /** Metadata for the episode currently loaded in the player. */
  @JsonClass(generateAdapter = true)
  data class NowPlayingEpisode(
    var title: String = "",
    var artUrl: String = "",
    var artUrl600: String = "",
    var mediaUrl: String = "",
    var description: String = "",
    var podcastTitle: String = "",
  ) {
    companion object {
      fun from(e: GoViewModel.REpisode, p: GoViewModel.RPodcast): NowPlayingEpisode {
        return NowPlayingEpisode(
          title = e.title ?: "",
          artUrl = p.imageUrl ?: "",
          artUrl600 = p.imageUrl600 ?: "",
          mediaUrl = e.mediaUrl ?: "",
          description = e.description ?: "",
          podcastTitle = p.feedTitle ?: "",
        )
      }
    }
  }

  private val _recentEpisode = MutableLiveData<NowPlayingEpisode?>()
  val recentEpisode: LiveData<NowPlayingEpisode?> = _recentEpisode

  private val _isPlaying = MutableLiveData(false)
  val isPlaying: LiveData<Boolean> = _isPlaying

  private val _episodeDuration = MutableLiveData<Long>()
  val episodeDuration: LiveData<Long> = _episodeDuration

  val formattedDuration: LiveData<String> = _episodeDuration.map {
    DateUtils.formatElapsedTime(it / 1000)
  }

  private val _playbackState = MutableLiveData(PlaybackStateCompat.STATE_NONE)
  val podcastTitleOrBuffering: LiveData<String> = _playbackState.map { state ->
    if (state == PlaybackStateCompat.STATE_BUFFERING) {
      "Buffering ..."
    } else {
      recentEpisode.value?.podcastTitle ?: "Loading ..."
    }
  }

  private val _currentTime = MutableLiveData(0L)
  val formattedCurrentTime: LiveData<String> = _currentTime.map {
    DateUtils.formatElapsedTime(it)
  }

  init {
    loadRecentEpisode()
  }

  private fun loadRecentEpisode() {
    viewModelScope.launch {
      _recentEpisode.value = goPreferences.latestEpisode
    }
  }

  fun saveRecentEpisode(episode: NowPlayingEpisode) {
    viewModelScope.launch {
      goPreferences.latestEpisode = episode
      loadRecentEpisode()
    }
  }

  fun setIsPlaying(playing: Boolean) {
    _isPlaying.value = playing
  }

  fun setEpisodeDuration(duration: Long) {
    _episodeDuration.value = if (duration < 0) 0 else duration
  }

  fun setCurrentTime(time: Long) {
    _currentTime.value = time
  }

  fun setPlayState(state: Int) {
    _playbackState.value = state
  }
}
