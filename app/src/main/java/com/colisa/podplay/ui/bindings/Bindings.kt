package com.colisa.podplay.ui.bindings

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colisa.podplay.ui.adapters.PodcastListAdapter
import com.colisa.podplay.ui.viewmodels.MainViewModel.PodcastSummary


@BindingAdapter("app:imageUrl")
fun setImageUrl(view: ImageView, url: String) {
    Glide.with(view)
        .load(url)
        .into(view)
}

@BindingAdapter("app:items")
fun setItems(view: RecyclerView, items: List<PodcastSummary>?) {
    items?.let {
        (view.adapter as PodcastListAdapter).submitList(items)
    }
}