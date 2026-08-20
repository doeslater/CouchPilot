package com.example.couchpilot.discover.presentation

import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val shows: List<TvShow>,
        val providers: List<WatchProvider> = emptyList(),
        val selectedProviderId: Int? = null
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}
