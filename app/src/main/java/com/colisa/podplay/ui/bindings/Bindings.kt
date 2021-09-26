package com.colisa.podplay.ui.bindings

import android.text.TextUtils
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colisa.podplay.R
import com.colisa.podplay.extensions.handleViewVisibility
import com.colisa.podplay.ui.GoViewModel
import com.colisa.podplay.ui.NowPlayingViewModel
import com.colisa.podplay.ui.adapters.EpisodeListAdapter
import com.colisa.podplay.ui.adapters.PodcastsListAdapter
import com.google.android.material.imageview.ShapeableImageView

/**
 * This adapter makes sure to load large images only when required,
 */
@BindingAdapter("app:imageUrl", "app:isLarge")
fun setImageUrl(view: ImageView, podcast: GoViewModel.IPodcast?, isLarge: Boolean?) {
    podcast?.let { ps ->
        isLarge?.let { lg ->
            if (lg) {
                Glide.with(view)
                    .load(ps.imageUrl600)
                    .thumbnail(Glide.with(view).load(podcast.imageUrl))
                    .into(view)
            } else {
                Glide.with(view)
                    .load(ps.imageUrl)
                    .into(view)
            }
        }
    }
}

@BindingAdapter("app:episodes")
fun setEpisodes(view: RecyclerView, items: List<GoViewModel.REpisode>?) {
    items?.let {
        (view.adapter as EpisodeListAdapter).addHeaderAndSubmitList(items)
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

@BindingAdapter("app:episodeTitle")
fun setEpisodeTitle(view: TextView, episode: NowPlayingViewModel.NowPlayingEpisode?) {
    if (episode != null) {
        view.text = episode.title
    } else {
        view.text = view.context.getString(R.string.app_name)
    }
}

@BindingAdapter("app:podcastTitle")
fun setPodcastTitle(view: TextView, episode: NowPlayingViewModel.NowPlayingEpisode?) {
    if (episode != null) {
        view.text = episode.podcastTitle
    } else {
        view.text = view.context.getString(R.string.version_name)
    }
}

@BindingAdapter("app:setPlayPauseSrc")
fun setPlayPauseSrc(view: ImageButton, isPlaying: Boolean?) {
    if (isPlaying == true) {
        view.setImageResource(R.drawable.ic_pause)
    } else {
        view.setImageResource(R.drawable.ic_play)
    }
}

@BindingAdapter("app:panelCoverArt")
fun setCover(view: ShapeableImageView, episode: NowPlayingViewModel.NowPlayingEpisode?) {
    if (episode == null) {
        Glide.with(view)
            .load(R.drawable.album_art)
            .into(view)
    } else {
        Glide.with(view)
            .load(episode.artUrl600)
            .into(view)
    }
}

@BindingAdapter("app:visibility")
fun setVisibility(view: View, isVisible: Boolean?) {
    view.isVisible = isVisible == true
}