package com.example.couchpilot.bookmarks.presentation

import com.example.couchpilot.tmdb.domain.TvShow

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState

    // An empty list is a valid, expected outcome (no bookmarks yet) rather than its own variant -
    // BookmarksScreen renders an empty-state message inline when shows.isEmpty().
    data class Success(val shows: List<TvShow>) : BookmarksUiState
}
