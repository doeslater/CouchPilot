package com.example.couchpilot.discover.presentation

import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

/** A [com.example.couchpilot.discover.domain.UkCultureCollection], lazily resolved - [shows] is
 *  null until its row actually scrolls into view (see DiscoverScreen's `LaunchedEffect` in
 *  `CollectionRow`), so the collections don't all fire a network call on screen load just because
 *  they're defined. Empty (not null) means the query ran and found nothing, or failed - either way
 *  the row hides itself rather than showing an empty shelf or an error that would take down the
 *  whole screen. [title] is the stable identity used both as the Lazy grid item key and to guard
 *  against double-fetching (see DiscoverViewModel.loadCollectionShows) - it's unique across
 *  [com.example.couchpilot.discover.domain.UK_CULTURE_COLLECTIONS] whether the collection is
 *  genre- or network-based. */
data class DiscoverCollection(
    val title: String,
    val minVoteCount: Int,
    val genreId: Int? = null,
    val networkId: Int? = null,
    val shows: List<TvShow>? = null,
)

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
