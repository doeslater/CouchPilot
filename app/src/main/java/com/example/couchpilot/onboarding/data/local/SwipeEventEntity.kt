package com.example.couchpilot.onboarding.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swipe_events")
data class SwipeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val showId: Int,
    val genreIds: String, // Comma-separated
    val liked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
