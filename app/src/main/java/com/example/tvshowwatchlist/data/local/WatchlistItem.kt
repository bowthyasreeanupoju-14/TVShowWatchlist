package com.example.tvshowwatchlist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: Int,
    val name: String,
    val posterUrl: String?,
    val rating: Double?,
    val summary: String?,
    val genres: String?,
    val language: String?,
    val premiered: String?,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
