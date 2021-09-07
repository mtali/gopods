package com.colisa.podplay.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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


}