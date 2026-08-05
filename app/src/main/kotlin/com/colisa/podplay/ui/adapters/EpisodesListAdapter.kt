package com.colisa.podplay.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.colisa.podplay.databinding.EpisodesDetailHeaderBinding
import com.colisa.podplay.databinding.ItemEpisodeBinding
import com.colisa.podplay.ui.GoViewModel
import timber.log.Timber
import kotlin.system.measureTimeMillis

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

class EpisodeListAdapter(private val goViewModel: GoViewModel) :
    ListAdapter<DataItem, ViewHolder>(DATA_ITEM_COMPARATOR) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DataItem.EpisodeItem -> ITEM_VIEW_TYPE_EPISODE
            else -> throw UnsupportedOperationException("Unknown view")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> EpisodesHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_EPISODE -> EpisodeViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let {
            when (it) {
                is DataItem.Header -> (holder as EpisodesHeaderViewHolder).bind(goViewModel)
                is DataItem.EpisodeItem -> (holder as EpisodeViewHolder).bind(
                    goViewModel,
                    it.episode
                )
            }
        }
    }

    fun addHeaderAndSubmitList(list: List<GoViewModel.REpisode>?) {
        val x = measureTimeMillis {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.EpisodeItem(it) }
            }
            submitList(items)
        }
        Timber.d("addHeaderAndSubmitList(count=${list?.count()}) took $x ms")
    }

    companion object {
        private val DATA_ITEM_COMPARATOR = object : DiffUtil.ItemCallback<DataItem>() {
            override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
                return (oldItem is DataItem.EpisodeItem &&
                        newItem is DataItem.EpisodeItem &&
                        oldItem.episode.guid == newItem.episode.guid
                        ) ||
                        (oldItem is DataItem.Header && newItem is DataItem.Header)
            }

            override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}

sealed class DataItem {
    data class EpisodeItem(val episode: GoViewModel.REpisode) : DataItem()
    object Header : DataItem()
}


class EpisodeViewHolder private constructor(private val binding: ItemEpisodeBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(goViewModel: GoViewModel, item: GoViewModel.REpisode) {
        binding.goViewModel = goViewModel
        binding.episode = item
        binding.executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup): EpisodeViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemEpisodeBinding.inflate(inflater, parent, false)
            return EpisodeViewHolder(binding)
        }
    }
}

class EpisodesHeaderViewHolder private constructor(private val binding: EpisodesDetailHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {


    fun bind(goViewModel: GoViewModel) {
        binding.goViewModel = goViewModel
        binding.executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup): EpisodesHeaderViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = EpisodesDetailHeaderBinding.inflate(inflater, parent, false)
            return EpisodesHeaderViewHolder(binding)
        }
    }

}




