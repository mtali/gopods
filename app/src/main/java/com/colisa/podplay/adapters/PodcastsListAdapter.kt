package com.colisa.podplay.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colisa.podplay.databinding.ItemPodcastBinding
import com.colisa.podplay.ui.GoViewModel
import com.colisa.podplay.ui.GoViewModel.DItunesPodcast


class PodcastsListAdapter(private val goViewModel: GoViewModel) :
    ListAdapter<DItunesPodcast, PodcastsListAdapter.ViewHolder>(PodcastDiffCallback()) {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(goViewModel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    class ViewHolder private constructor(private val binding: ItemPodcastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(viewmodel: GoViewModel, podcast: DItunesPodcast) {
            binding.goViewModel = viewmodel
            binding.podcast = podcast
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemPodcastBinding.inflate(inflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class PodcastDiffCallback : DiffUtil.ItemCallback<DItunesPodcast>() {
    override fun areItemsTheSame(oldItem: DItunesPodcast, newItem: DItunesPodcast): Boolean {
        return oldItem.feedUrl == newItem.feedUrl
    }

    override fun areContentsTheSame(oldItem: DItunesPodcast, newItem: DItunesPodcast): Boolean {
        return oldItem == newItem
    }
}