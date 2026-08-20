package com.example.couchpilot.tmdb.domain

/** Domain model for a TV show, mapped from TMDB's DTOs — never expose TMDB's DTOs above the data layer. */
data class TvShow(
    val id: Int,
    val name: String,
    val overview: String,
    val posterUrl: String?,
    val voteAverage: Double,
    val firstAirDate: String?,
    val genreIds: List<Int> = emptyList(),
)
