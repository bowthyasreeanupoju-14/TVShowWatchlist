package com.example.tvshowwatchlist

import com.example.tvshowwatchlist.data.local.WatchlistItem
import com.example.tvshowwatchlist.data.model.Rating
import com.example.tvshowwatchlist.data.model.TvImage
import com.example.tvshowwatchlist.data.model.TvShow
import com.example.tvshowwatchlist.data.model.TvShowSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TvShowModelTest {

    @Test
    fun testTvShowDataMapping() {
        val show = TvShow(
            id = 1,
            name = "Breaking Bad",
            summary = "<p>A chemistry teacher turned manufacturer.</p>",
            language = "English",
            premiered = "2008-01-20",
            rating = Rating(average = 9.5),
            image = TvImage(medium = "https://example.com/medium.jpg", original = "https://example.com/original.jpg"),
            genres = listOf("Drama", "Crime", "Thriller")
        )

        val searchResult = TvShowSearchResult(score = 0.98, show = show)

        assertEquals(1, searchResult.show.id)
        assertEquals("Breaking Bad", searchResult.show.name)
        assertEquals(9.5, searchResult.show.rating?.average)
        assertEquals("https://example.com/medium.jpg", searchResult.show.image?.medium)
        assertEquals(3, searchResult.show.genres?.size)
    }

    @Test
    fun testWatchlistItemCreation() {
        val item = WatchlistItem(
            id = 1,
            name = "Breaking Bad",
            posterUrl = "https://example.com/medium.jpg",
            rating = 9.5,
            summary = "A chemistry teacher turned manufacturer.",
            genres = "Drama, Crime, Thriller",
            language = "English",
            premiered = "2008-01-20",
            status = "Watching"
        )

        assertEquals(1, item.id)
        assertEquals("Breaking Bad", item.name)
        assertEquals("Watching", item.status)
        assertNotNull(item.timestamp)
    }
}
