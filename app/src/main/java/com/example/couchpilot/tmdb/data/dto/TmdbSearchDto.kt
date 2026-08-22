package com.example.couchpilot.tmdb.data.dto

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponseDto(
    val results: List<TmdbSearchResultDto> = emptyList()
)

data class TmdbSearchResultDto(
    val id: Int,
    val name: String?,
    val title: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("media_type") val mediaType: String?
)
