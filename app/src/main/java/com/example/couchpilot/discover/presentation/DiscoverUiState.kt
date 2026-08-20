package com.example.couchpilot.discover.presentation

import com.example.couchpilot.tmdb.domain.TvShow

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(val shows: List<TvShow>) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}
