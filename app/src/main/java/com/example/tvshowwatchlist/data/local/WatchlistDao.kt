package com.example.tvshowwatchlist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY timestamp DESC")
    fun getAllWatchlist(): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist WHERE status = :status ORDER BY timestamp DESC")
    fun getWatchlistByStatus(status: String): Flow<List<WatchlistItem>>

    @Query("SELECT * FROM watchlist WHERE id = :id LIMIT 1")
    suspend fun getWatchlistItemById(id: Int): WatchlistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: WatchlistItem): Long

    @Delete
    suspend fun delete(item: WatchlistItem): Int

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: Int): Int
}
