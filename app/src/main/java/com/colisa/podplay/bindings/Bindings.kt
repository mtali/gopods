package com.colisa.podplay.bindings

import android.text.TextUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colisa.podplay.adapters.EpisodeListAdapter
import com.colisa.podplay.adapters.PodcastsListAdapter
import com.colisa.podplay.extensions.handleViewVisibility
import com.colisa.podplay.ui.GoViewModel


@BindingAdapter("app:imageUrl")
fun setImageUrl(view: ImageView, url: String?) {
    url?.let {
        Glide.with(view)
            .load(url)
            .into(view)
    }
}

@BindingAdapter("app:episodes")
fun setEpisodes(view: RecyclerView, items: List<GoViewModel.REpisode>?) {
    items?.let {
        (view.adapter as EpisodeListAdapter).submitList(items)
    }
}

@BindingAdapter("app:loading")
fun setLoadingState(view: ProgressBar, loading: Boolean?) {
    val show = loading == true
    view.handleViewVisibility(show)
}

@BindingAdapter("app:podcasts")
fun setDItunesPodcasts(view: RecyclerView, podcasts: List<GoViewModel.IPodcast>?) {
    podcasts?.let {
        (view.adapter as PodcastsListAdapter).submitList(podcasts)
    }
}

@BindingAdapter("app:marquee_text")
fun setMarqueeText(view: TextView, text: String?) {
    text?.let {
        view.text = text
        view.ellipsize = TextUtils.TruncateAt.MARQUEE
        view.isSingleLine = true
        view.marqueeRepeatLimit = -1
        view.isSelected = true
    }
}