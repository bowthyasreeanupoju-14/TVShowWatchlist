package com.example.tvshowwatchlist.data.repository

import com.example.tvshowwatchlist.data.api.TvMazeApiService
import com.example.tvshowwatchlist.data.local.WatchlistDao
import com.example.tvshowwatchlist.data.local.WatchlistItem
import com.example.tvshowwatchlist.data.model.TvShow
import com.example.tvshowwatchlist.data.model.TvShowSearchResult
import kotlinx.coroutines.flow.Flow

class TvRepository(
    private val apiService: TvMazeApiService,
    private val watchlistDao: WatchlistDao
) {
    suspend fun searchShows(query: String): List<TvShowSearchResult> {
        return apiService.searchShows(query)
    }

    suspend fun getShowDetails(id: Int): TvShow {
        return apiService.getShowDetails(id)
    }

    fun getAllWatchlist(): Flow<List<WatchlistItem>> {
        return watchlistDao.getAllWatchlist()
    }

    fun getWatchlistByStatus(status: String): Flow<List<WatchlistItem>> {
        return watchlistDao.getWatchlistByStatus(status)
    }

    suspend fun getWatchlistItemById(id: Int): WatchlistItem? {
        return watchlistDao.getWatchlistItemById(id)
    }

    suspend fun saveToWatchlist(item: WatchlistItem): Long {
        return watchlistDao.insertOrUpdate(item)
    }

    suspend fun removeFromWatchlist(item: WatchlistItem): Int {
        return watchlistDao.delete(item)
    }

    suspend fun removeFromWatchlistById(id: Int): Int {
        return watchlistDao.deleteById(id)
    }
}
