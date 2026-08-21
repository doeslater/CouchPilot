package com.example.couchpilot.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val preferencesRepository: PreferencesRepository,
    private val swipeEventDao: SwipeEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadShows()
    }

    private fun loadShows() {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            tmdbRepository.getTrendingTvShows(null)
                .onSuccess { shows ->
                    _uiState.value = OnboardingUiState.Success(shows = shows.take(15))
                }
                .onFailure { error ->
                    _uiState.value = OnboardingUiState.Error(error.toString())
                }
        }
    }

    fun onSwipe(show: TvShow, liked: Boolean) {
        viewModelScope.launch {
            swipeEventDao.insertSwipeEvent(
                SwipeEventEntity(
                    showId = show.id,
                    genreIds = show.genreIds.joinToString(","),
                    liked = liked
                )
            )
            advanceToNext()
        }
    }

    fun onSkipShow() {
        advanceToNext()
    }

    private fun advanceToNext() {
        _uiState.update { state ->
            if (state is OnboardingUiState.Success) {
                val nextIndex = state.currentIndex + 1
                if (nextIndex >= state.shows.size) {
                    completeOnboarding()
                }
                state.copy(currentIndex = nextIndex)
            } else state
        }
    }

    /**
     * Bails out of the swipe deck early with no preference signal recorded — Tonight/Discover
     * fall back to their cold-start (rating-only) ordering, same as a freshly-onboarded user who
     * swiped on nothing overlapping the current lists (see RecommendationScorer/Phase 5).
     */
    fun skipOnboarding() {
        completeOnboarding()
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }
}
