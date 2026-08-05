package com.colisa.podplay.ui

import android.app.Application
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.colisa.podplay.goPreferences
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.launch

class NowPlayingViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Utility class used to represent the metadata for the
     * current episode being played
     */
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
            /**
             * Convert REpisode to NowPlayingEpisode
             */
            fun from(e: GoViewModel.REpisode, p: GoViewModel.RPodcast): NowPlayingEpisode {
                return NowPlayingEpisode(
                    title = e.title ?: "",
                    artUrl = p.imageUrl ?: "",
                    artUrl600 = p.imageUrl600 ?: "",
                    mediaUrl = e.mediaUrl ?: "",
                    description = e.description ?: "",
                    podcastTitle = p.feedTitle ?: ""
                )
            }
        }
    }

    private val _recentEpisode = MutableLiveData<NowPlayingEpisode?>()
    val recentEpisode: LiveData<NowPlayingEpisode?> = _recentEpisode

    private val _isPlaying = MutableLiveData<Boolean>(false)
    var isPlaying: LiveData<Boolean> = _isPlaying

    private val _episodeDuration = MutableLiveData<Long>()
    val episodeDuration: LiveData<Long> = _episodeDuration

    val formattedDuration: LiveData<String> = _episodeDuration.map {
        DateUtils.formatElapsedTime(it / 1000)
    }

    private val _playbackState = MutableLiveData<Int>(PlaybackStateCompat.STATE_NONE)
    val podcastTitleOrBuffering: LiveData<String> = _playbackState.map { state ->
        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            "Buffering ..."
        } else {
            val title = recentEpisode.value?.podcastTitle
            title ?: "Loading ..."

        }
    }

    private var _currentTime = MutableLiveData<Long>(0)
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
        _episodeDuration.value = if (duration < 0) {
            0
        } else {
            duration
        }
    }

    fun setCurrentTime(time: Long) {
        _currentTime.value = time
    }

    fun setPlayState(state: Int) {
        _playbackState.value = state
    }


}