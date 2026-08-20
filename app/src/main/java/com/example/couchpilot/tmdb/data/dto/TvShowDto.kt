package com.example.couchpilot.tmdb.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingTvShowsResponseDto(
    val results: List<TvShowDto> = emptyList(),
)

@Serializable
data class TvShowDto(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("first_air_date") val firstAirDate: String? = null,
)
