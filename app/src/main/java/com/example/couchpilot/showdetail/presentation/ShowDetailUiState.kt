package com.example.couchpilot.showdetail.presentation

import com.example.couchpilot.tmdb.domain.TvShow

sealed interface ShowDetailUiState {
    data object Loading : ShowDetailUiState
    data class Success(val show: TvShow) : ShowDetailUiState
    data class Error(val message: String) : ShowDetailUiState
}
