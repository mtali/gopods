package com.colisa.podplay.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colisa.podplay.databinding.ItemEpisodeBinding
import com.colisa.podplay.ui.viewmodels.PodcastDetailViewModel

class EpisodeListAdapter(private val viewmodel: PodcastDetailViewModel) :
    ListAdapter<PodcastDetailViewModel.EpisodeOnView, EpisodeListAdapter.ViewHolder>(
        EpisodeDiffCallback()
    ) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(viewmodel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(viewmodel: PodcastDetailViewModel, item: PodcastDetailViewModel.EpisodeOnView) {
            binding.viewmodel = viewmodel
            binding.episode = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemEpisodeBinding.inflate(inflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class EpisodeDiffCallback : DiffUtil.ItemCallback<PodcastDetailViewModel.EpisodeOnView>() {
    override fun areItemsTheSame(
        oldItem: PodcastDetailViewModel.EpisodeOnView,
        newItem: PodcastDetailViewModel.EpisodeOnView
    ): Boolean {
        return oldItem.guid == newItem.guid
    }

    override fun areContentsTheSame(
        oldItem: PodcastDetailViewModel.EpisodeOnView,
        newItem: PodcastDetailViewModel.EpisodeOnView
    ): Boolean {
        return oldItem == newItem
    }
}
