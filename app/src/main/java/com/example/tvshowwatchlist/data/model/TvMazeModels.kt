package com.example.tvshowwatchlist.data.model

import com.google.gson.annotations.SerializedName

data class TvShowSearchResult(
    @SerializedName("score") val score: Double? = null,
    @SerializedName("show") val show: TvShow
)

data class TvShow(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("premiered") val premiered: String? = null,
    @SerializedName("rating") val rating: Rating? = null,
    @SerializedName("image") val image: TvImage? = null,
    @SerializedName("genres") val genres: List<String>? = null
)

data class Rating(
    @SerializedName("average") val average: Double? = null
)

data class TvImage(
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("original") val original: String? = null
)
