package com.colisa.podplay.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.colisa.podplay.databinding.ItemEpisodeBinding
import com.colisa.podplay.ui.GoViewModel

class EpisodeListAdapter(private val goViewModel: GoViewModel) :
    ListAdapter<GoViewModel.REpisode, EpisodeListAdapter.ViewHolder>(
        EpisodeDiffCallback()
    ) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(goViewModel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(goViewModel: GoViewModel, item: GoViewModel.REpisode) {
            binding.goViewModel = goViewModel
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

class EpisodeDiffCallback : DiffUtil.ItemCallback<GoViewModel.REpisode>() {
    override fun areItemsTheSame(
        oldItem: GoViewModel.REpisode,
        newItem: GoViewModel.REpisode
    ): Boolean {
        return oldItem.guid == newItem.guid
    }

    override fun areContentsTheSame(
        oldItem: GoViewModel.REpisode,
        newItem: GoViewModel.REpisode
    ): Boolean {
        return oldItem.mediaUrl == newItem.mediaUrl
    }
}
