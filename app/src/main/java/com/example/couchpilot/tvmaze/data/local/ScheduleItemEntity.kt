package com.example.couchpilot.tvmaze.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItemEntity(
    @PrimaryKey val id: Int,
    val showId: Int,
    val showName: String,
    val episodeName: String?,
    val airtime: String?,
    val runtime: Int?,
    val channel: String?,
    val summary: String?,
    val imdbId: String?,
    val posterUrl: String?,
    val date: String, // YYYY-MM-DD
    val rating: Double? = null,
    // Comma-joined, same convention as TvShowEntity.genreIds - no List<Int> TypeConverter yet.
    val genreIds: String = "",
    val tmdbId: Int? = null,
)
