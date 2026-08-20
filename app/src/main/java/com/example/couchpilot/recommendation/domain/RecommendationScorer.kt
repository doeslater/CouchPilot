package com.example.couchpilot.recommendation.domain

import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Calculates a match score [0.0..1.0] for a list of genres based on user swipe history.
 * Uses cosine similarity: dot(user, show) / (||user|| * ||show||).
 */
@Singleton
class RecommendationScorer @Inject constructor(
    private val swipeEventDao: SwipeEventDao
) {
    suspend fun computePreferenceVector(): PreferenceVector {
        val events = swipeEventDao.getAllSwipeEvents().first()
        if (events.isEmpty()) return PreferenceVector()

        val weights = mutableMapOf<Int, Double>()
        events.forEach { event ->
            val delta = if (event.liked) 1.0 else -1.0
            event.genreIds.split(",")
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
                .forEach { genreId ->
                    weights[genreId] = (weights[genreId] ?: 0.0) + delta
                }
        }
        return PreferenceVector(weights)
    }

    /**
     * Scores a show's genre list against the user's taste.
     * Higher is better. 0.0 if no overlap or no preferences.
     */
    fun score(genreIds: List<Int>, userTaste: PreferenceVector): Double {
        if (userTaste.isEmpty() || genreIds.isEmpty()) return 0.0

        // Show vector is just 1.0 for each genre it has
        val dotProduct = genreIds.sumOf { userTaste.weights[it] ?: 0.0 }
        val showMagnitude = sqrt(genreIds.size.toDouble())
        
        val similarity = dotProduct / (userTaste.magnitude * showMagnitude)
        
        // Normalize cosine similarity [-1..1] to [0..1]
        return (similarity + 1.0) / 2.0
    }
}
