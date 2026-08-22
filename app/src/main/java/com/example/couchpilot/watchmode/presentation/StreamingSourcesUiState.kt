package com.example.couchpilot.watchmode.presentation

import com.example.couchpilot.watchmode.domain.WatchmodeSource

sealed interface StreamingSourcesUiState {
    object Loading : StreamingSourcesUiState
    data class Success(val sources: List<WatchmodeSource>) : StreamingSourcesUiState
    data class Error(val message: String) : StreamingSourcesUiState
}
