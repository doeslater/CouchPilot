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
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),
)

/**
 * TMDB's single-show `/tv/{id}` endpoint, NOT the same shape as list/trending/discover
 * endpoints: genres come back as `[{id, name}]` objects here, not a flat `genre_ids: [int]`.
 */
data class TvShowDetailDto(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double = 0.0,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    val genres: List<GenreDto> = emptyList(),
)

data class GenreDto(
    val id: Int,
    val name: String,
)
