package com.example.couchpilot.showdetail.presentation

import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

sealed interface ShowDetailUiState {
    data object Loading : ShowDetailUiState
    data class Success(
        val show: TvShow,
        val providers: List<WatchProvider> = emptyList(),
        // null until the user taps thumbs up/down this session - not persisted/reloaded as its
        // own field, the vote itself already landed in swipe_events via onVote().
        val userVote: Boolean? = null
    ) : ShowDetailUiState
    data class Error(val message: String) : ShowDetailUiState
}
