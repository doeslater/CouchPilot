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
    // TVmaze's own show rating (0-10). Used today as a stand-in "recommendation" signal
    // (see recommendation/domain/ScheduleRanking.kt) until Phases 4/5 land real preference data.
    val rating: Double? = null,
)
