package com.example.couchpilot.showdetail.presentation

import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

sealed interface ShowDetailUiState {
    data object Loading : ShowDetailUiState
    data class Success(
        val show: TvShow,
        val providers: List<WatchProvider> = emptyList()
    ) : ShowDetailUiState
    data class Error(val message: String) : ShowDetailUiState
}
