package com.example.couchpilot.discover.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.WatchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DiscoverViewModel"

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var allProviders: List<WatchProvider> = emptyList()

    // Tracks the in-flight trending fetch so a rapid second filter tap can cancel the first
    // one instead of letting an out-of-order response overwrite the newer selection.
    private var loadTrendingJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading

            // Providers and trending shows are independent calls - fetch them concurrently
            // (not one after the other) so the spinner shows for max(latency), not the sum.
            val providersDeferred = async {
                tmdbRepository.getWatchProviders()
                    .onSuccess { providers -> allProviders = providers }
                    .onFailure { error ->
                        // Non-fatal: the screen still works with just the "All" chip if this
                        // fails, but it shouldn't fail silently with no trace at all.
                        Log.w(TAG, "Failed to load watch providers, filter chips will be empty: $error")
                    }
            }
            val showsResult = tmdbRepository.getTrendingTvShows(null)
            providersDeferred.await()

            showsResult
                .onSuccess { shows ->
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = null,
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }

    fun selectProvider(providerId: Int?) {
        loadTrending(providerId)
    }

    private fun loadTrending(providerId: Int?) {
        loadTrendingJob?.cancel()
        loadTrendingJob = viewModelScope.launch {
            // Keep current success state if possible to avoid flickering while filtering
            val currentState = _uiState.value
            if (currentState !is DiscoverUiState.Success) {
                _uiState.value = DiscoverUiState.Loading
            } else {
                _uiState.update {
                    (it as DiscoverUiState.Success).copy(selectedProviderId = providerId)
                }
            }

            tmdbRepository.getTrendingTvShows(providerId)
                .onSuccess { shows ->
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = providerId,
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }
}
