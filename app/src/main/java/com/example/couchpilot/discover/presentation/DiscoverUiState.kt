package com.example.couchpilot.discover.presentation

import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

/** A resolved [com.example.couchpilot.discover.domain.UkCultureCollection] - real [TvShow]s from
 *  TMDB's genre discover query. Empty (not an error) if that query fails, so one bad collection
 *  doesn't take down the whole screen. */
data class DiscoverCollection(val title: String, val shows: List<TvShow>)

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val shows: List<TvShow>,
        val providers: List<WatchProvider> = emptyList(),
        val selectedProviderId: Int? = null,
        // Loaded once at init and preserved across provider-filter changes (loadTrending() only
        // ever re-fetches `shows`) - collections are static curated content, not provider-scoped.
        val collections: List<DiscoverCollection> = emptyList(),
        // True only when the "All" chip (not "Collections") is selected - same
        // providerId=null trending query as "Collections", but deliberately hides collections
        // too, reproducing the screen's pre-collections appearance for anyone who preferred that
        // simpler view.
        val isClassicSelected: Boolean = false
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}
