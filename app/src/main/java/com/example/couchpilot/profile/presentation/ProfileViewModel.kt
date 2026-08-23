package com.example.couchpilot.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import com.example.couchpilot.recommendation.domain.TmdbTvGenres
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Surfaces the same PreferenceVector that RecommendationScorer builds internally for ranking
 * Tonight/Discover, plus raw swipe/vote counts from SwipeEventDao, so the user can actually see
 * what "learning your taste" has picked up on (GENERAL_IDEA.md's "Privacy-Preserving AI Sync" -
 * this is the one place that sync becomes visible instead of only ever acting behind the scenes).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val recommendationScorer: RecommendationScorer,
    private val swipeEventDao: SwipeEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val events = swipeEventDao.getAllSwipeEvents().first()
            val taste = recommendationScorer.computePreferenceVector()
            val genreAffinities = taste.weights
                .map { (genreId, weight) -> GenreAffinity(TmdbTvGenres.nameFor(genreId), weight) }
                .sortedByDescending { it.weight }

            _uiState.value = ProfileUiState.Success(
                totalSwipes = events.size,
                likedCount = events.count { it.liked },
                dislikedCount = events.count { !it.liked },
                genreAffinities = genreAffinities
            )
        }
    }
}
