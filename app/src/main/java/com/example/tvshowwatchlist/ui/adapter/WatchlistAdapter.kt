package com.example.tvshowwatchlist.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvshowwatchlist.data.local.WatchlistItem
import com.example.tvshowwatchlist.databinding.ItemWatchlistBinding

class WatchlistAdapter(
    private val onItemClick: (WatchlistItem) -> Unit
) : ListAdapter<WatchlistItem, WatchlistAdapter.WatchlistViewHolder>(WatchlistDiffCallback) {

    fun getItemAt(position: Int): WatchlistItem {
        return getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchlistViewHolder {
        val binding = ItemWatchlistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WatchlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WatchlistViewHolder(private val binding: ItemWatchlistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WatchlistItem) {
            binding.tvTitle.text = item.name
            
            val ratingText = item.rating?.let { "⭐ %.1f / 10".format(it) } ?: "⭐ N/A"
            binding.tvRating.text = ratingText

            binding.tvStatus.text = item.status
            val statusColor = when (item.status) {
                "Plan to Watch" -> Color.parseColor("#1E88E5")
                "Watching" -> Color.parseColor("#FB8C00")
                "Completed" -> Color.parseColor("#43A047")
                else -> Color.parseColor("#6200EE")
            }
            binding.tvStatus.background.setTint(statusColor)

            Glide.with(binding.ivPoster.context)
                .load(item.posterUrl)
                .centerCrop()
                .into(binding.ivPoster)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    object WatchlistDiffCallback : DiffUtil.ItemCallback<WatchlistItem>() {
        override fun areItemsTheSame(oldItem: WatchlistItem, newItem: WatchlistItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: WatchlistItem, newItem: WatchlistItem): Boolean =
            oldItem == newItem
    }
}
