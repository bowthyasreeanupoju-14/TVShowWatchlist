package com.example.tvshowwatchlist.data.api

import com.example.tvshowwatchlist.data.model.TvShow
import com.example.tvshowwatchlist.data.model.TvShowSearchResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvMazeApiService {

    @GET("search/shows")
    suspend fun searchShows(@Query("q") query: String): List<TvShowSearchResult>

    @GET("shows/{id}")
    suspend fun getShowDetails(@Path("id") id: Int): TvShow

    companion object {
        private const val BASE_URL = "https://api.tvmaze.com/"

        fun create(): TvMazeApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TvMazeApiService::class.java)
        }
    }
}
