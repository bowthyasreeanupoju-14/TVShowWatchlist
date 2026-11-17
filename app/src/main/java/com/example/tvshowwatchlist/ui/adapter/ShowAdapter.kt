package com.example.tvshowwatchlist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvshowwatchlist.data.model.TvShow
import com.example.tvshowwatchlist.databinding.ItemShowGridBinding

class ShowAdapter(
    private val onShowClick: (TvShow) -> Unit
) : ListAdapter<TvShow, ShowAdapter.ShowViewHolder>(ShowDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowViewHolder {
        val binding = ItemShowGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShowViewHolder(private val binding: ItemShowGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(show: TvShow) {
            binding.tvTitle.text = show.name
            
            val ratingText = show.rating?.average?.let { "⭐ %.1f / 10".format(it) } ?: "⭐ N/A"
            binding.tvRating.text = ratingText

            val imageUrl = show.image?.medium ?: show.image?.original
            Glide.with(binding.ivPoster.context)
                .load(imageUrl)
                .centerCrop()
                .into(binding.ivPoster)

            binding.root.setOnClickListener {
                onShowClick(show)
            }
        }
    }

    object ShowDiffCallback : DiffUtil.ItemCallback<TvShow>() {
        override fun areItemsTheSame(oldItem: TvShow, newItem: TvShow): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TvShow, newItem: TvShow): Boolean =
            oldItem == newItem
    }
}
