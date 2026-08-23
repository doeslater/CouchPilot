package com.example.couchpilot.bookmarks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A show the user has explicitly saved for later, independent of the up/downvote signal recorded
 * in `swipe_events` - bookmarking is "I want to come back to this," not a taste-training decision,
 * so it deliberately doesn't feed [com.example.couchpilot.recommendation.domain.RecommendationScorer].
 *
 * Keyed by [showId] itself (not an autogenerate id) since a show is either bookmarked or not -
 * there's no meaningful history of repeat bookmark events the way swipes/votes have.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val showId: Int,
    val timestamp: Long = System.currentTimeMillis()
)
