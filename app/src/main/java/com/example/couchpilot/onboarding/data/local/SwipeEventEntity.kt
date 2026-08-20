package com.example.couchpilot.onboarding.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swipe_events")
data class SwipeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val showId: Int,
    val genreIds: String, // Comma-separated
    val liked: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    // How strongly this event should count toward the preference vector. Explicit signals
    // (onboarding swipes, ShowDetail up/downvotes) are 1.0 - a deliberate, unambiguous choice.
    // Dwell-time ("stayed on this show's detail screen a while") is a weak, down-weighted
    // signal - distraction isn't interest, so it shouldn't move the vector as much as a real
    // decision does. See RecommendationScorer.computePreferenceVector().
    val weight: Double = 1.0
)
