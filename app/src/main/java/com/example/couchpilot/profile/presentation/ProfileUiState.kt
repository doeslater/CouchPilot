package com.example.couchpilot.profile.presentation

/** One genre's contribution to the user's taste vector - see RecommendationScorer.PreferenceVector. */
data class GenreAffinity(
    val genreName: String,
    val weight: Double
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val totalSwipes: Int,
        val likedCount: Int,
        val dislikedCount: Int,
        val genreAffinities: List<GenreAffinity>
    ) : ProfileUiState {
        val isEmpty: Boolean get() = totalSwipes == 0
    }
}
