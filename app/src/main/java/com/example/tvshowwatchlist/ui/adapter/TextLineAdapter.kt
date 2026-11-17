package com.example.tvshowwatchlist.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tvshowwatchlist.databinding.ItemTextLineBinding

class TextLineAdapter(
    private val onLineClick: (String) -> Unit
) : ListAdapter<String, TextLineAdapter.TextLineViewHolder>(StringDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextLineViewHolder {
        val binding = ItemTextLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TextLineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TextLineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TextLineViewHolder(private val binding: ItemTextLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(text: String) {
            binding.tvTextLine.text = text
            binding.root.setOnClickListener {
                onLineClick(text)
            }
        }
    }

    object StringDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }
}
