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
        val userVote: Boolean? = null,
        // Set only when reached via a Discover provider chip AND AppLauncher actually has a
        // website search mapped for that provider - see Route.ShowDetail's doc comment. Drives
        // ShowDetailScreen's chip-origin CTA; absent otherwise so the screen doesn't render a
        // button that would just silently no-op when tapped.
        val originProviderName: String? = null,
        // Loaded alongside the show/providers in loadShowDetails() and flipped by
        // onToggleBookmark() - independent of userVote, since "save for later" isn't a taste signal.
        val isBookmarked: Boolean = false
    ) : ShowDetailUiState
    data class Error(val message: String) : ShowDetailUiState
}
