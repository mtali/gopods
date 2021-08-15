package com.colisa.podplay.ui.bindings

import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colisa.podplay.R
import com.colisa.podplay.ui.adapters.EpisodeListAdapter
import com.colisa.podplay.ui.adapters.PodcastListAdapter
import com.colisa.podplay.ui.viewmodels.MainViewModel.PodcastSummary
import com.colisa.podplay.ui.viewmodels.PodcastViewModel
import timber.log.Timber


@BindingAdapter("app:imageUrl")
fun setImageUrl(view: ImageView, url: String?) {
    url?.let {
        Glide.with(view)
            .load(url)
            .into(view)
    }
}

@BindingAdapter("app:items")
fun setItems(view: RecyclerView, items: List<PodcastSummary>?) {
    items?.let {
        (view.adapter as PodcastListAdapter).submitList(items)
    }
}

@BindingAdapter("app:episodes")
fun setEpisodes(view: RecyclerView, items: List<PodcastViewModel.EpisodeOnView>?) {
    items?.let {
        (view.adapter as EpisodeListAdapter).submitList(items)
    }
}

@BindingAdapter("app:playState")
fun setPlayState(view: ImageView, state: Int?) {
    Timber.d("State playing: ${state == STATE_PLAYING}")
    if (state == STATE_PLAYING) {
        view.setImageResource(R.drawable.ic_outline_pause_24)
    } else {
        view.setImageResource(R.drawable.ic_outline_play_arrow_24)
    }
}