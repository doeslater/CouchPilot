package com.example.couchpilot.tvmaze.domain

data class ScheduleItem(
    val id: Int,
    val showId: Int,
    val showName: String,
    val episodeName: String?,
    val airtime: String?,
    val runtime: Int?,
    val channel: String?,
    val summary: String?,
    val imdbId: String?,
    val posterUrl: String? = null,
    // TVmaze's own show rating (0-10) - used as the cold-start/fallback sort key when there's
    // no swipe signal yet, and as a tie-breaker once there is (see DefaultTvMazeRepository).
    val rating: Double? = null,
    // TMDB genre IDs, NOT TVmaze's (TVmaze exposes free-text genre names, a different
    // vocabulary entirely - incompatible with RecommendationScorer, which is trained on TMDB
    // genre IDs from swipe history). Always empty until TonightViewModel.enrichSchedule()
    // bridges this show to TMDB and copies its genreIds in.
    val genreIds: List<Int> = emptyList(),
    // The TMDB show id (distinct from showId, which is TVmaze's own). Always null until
    // TonightViewModel.enrichSchedule() bridges this show to TMDB - needed to navigate to
    // Route.ShowDetail, which takes a TMDB id.
    val tmdbId: Int? = null,
)
