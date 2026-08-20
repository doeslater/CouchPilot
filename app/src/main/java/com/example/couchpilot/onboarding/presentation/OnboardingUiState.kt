package com.example.couchpilot.onboarding.presentation

import com.example.couchpilot.tmdb.domain.TvShow

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Success(
        val shows: List<TvShow>,
        val currentIndex: Int = 0
    ) : OnboardingUiState {
        val currentShow: TvShow? get() = shows.getOrNull(currentIndex)
        val isFinished: Boolean get() = currentIndex >= shows.size
    }
    data class Error(val message: String) : OnboardingUiState
}
