package com.example.couchpilot.tmdb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String,
    val posterUrl: String?,
    val voteAverage: Double,
    val firstAirDate: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
