package com.colisa.podplay.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colisa.podplay.databinding.ItemPodcastBinding
import com.colisa.podplay.ui.viewmodels.MainViewModel
import com.colisa.podplay.ui.viewmodels.MainViewModel.PodcastSummary


class PodcastListAdapter(private val viewmodel: MainViewModel) :
    ListAdapter<PodcastSummary, PodcastListAdapter.ViewHolder>(PodcastDiffCallback()) {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(viewmodel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    class ViewHolder private constructor(private val binding: ItemPodcastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(viewmodel: MainViewModel, item: PodcastSummary) {
            binding.viewmodel = viewmodel
            binding.podcast = item
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

class PodcastDiffCallback : DiffUtil.ItemCallback<PodcastSummary>() {
    override fun areItemsTheSame(oldItem: PodcastSummary, newItem: PodcastSummary): Boolean {
        return oldItem.feedUrl == newItem.feedUrl
    }

    override fun areContentsTheSame(oldItem: PodcastSummary, newItem: PodcastSummary): Boolean {
        return oldItem == newItem
    }
}