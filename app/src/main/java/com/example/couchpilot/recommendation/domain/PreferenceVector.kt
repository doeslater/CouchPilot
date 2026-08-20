package com.example.couchpilot.recommendation.domain

import kotlin.math.sqrt

/**
 * A user's taste as a sparse vector of GenreID -> Weight.
 * Weights are Double values where positive means like, negative means dislike.
 */
data class PreferenceVector(
    val weights: Map<Int, Double> = emptyMap()
) {
    /**
     * Magnitude of the vector for cosine similarity normalization.
     */
    val magnitude: Double by lazy {
        sqrt(weights.values.sumOf { it * it })
    }

    fun isEmpty() = weights.isEmpty()
}
