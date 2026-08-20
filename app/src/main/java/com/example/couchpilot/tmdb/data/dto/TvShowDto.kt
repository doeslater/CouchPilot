package com.example.couchpilot.tmdb.data.dto

import com.google.gson.annotations.SerializedName

data class TrendingTvShowsResponseDto(
    val results: List<TvShowDto> = emptyList(),
)

data class TvShowDto(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
)
