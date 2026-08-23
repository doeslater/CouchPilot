package com.example.couchpilot.discover.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.discover.domain.UK_CULTURE_COLLECTIONS
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.WatchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DiscoverViewModel"
private const val COLLECTION_SHOW_LIMIT = 8

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var allProviders: List<WatchProvider> = emptyList()

    // Tracks the in-flight trending fetch so a rapid second filter tap can cancel the first
    // one instead of letting an out-of-order response overwrite the newer selection.
    private var loadTrendingJob: Job? = null

    // Guards loadCollectionShows() against firing the same query twice - a collection's row
    // composable can enter composition more than once (e.g. scrolled far away and back, which
    // disposes and later recreates offscreen Lazy items), but the network call itself should only
    // ever happen once per collection. Keyed by title (unique across UK_CULTURE_COLLECTIONS,
    // regardless of whether the collection is genre- or network-based) rather than genreId, since
    // a network-based collection has no genreId at all.
    private val requestedTitles = mutableSetOf<String>()

    init {
        loadInitialData()
    }

    /** The 12 UK culture collections, freshly defined with shows = null - i.e. "not yet
     *  hydrated." Used both for the very first Success state and as a fallback when a later
     *  Success state has to be reconstructed from scratch (see loadTrending's onSuccess). */
    private fun defaultCollections(): List<DiscoverCollection> =
        UK_CULTURE_COLLECTIONS.map { DiscoverCollection(it.title, it.minVoteCount, it.genreId, it.networkId) }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading

            // Providers and trending shows are independent calls - fetch them concurrently
            // (not one after the other) so the spinner shows for max(latency), not the sum.
            // Collections are NOT fetched here - each is defined instantly with shows = null and
            // only actually queried once its row scrolls into view (see loadCollectionShows()),
            // so opening Discover doesn't fire 8 genre-discover calls whether or not the user
            // ever scrolls down to see them.
            val providersDeferred = async {
                tmdbRepository.getWatchProviders()
                    .onSuccess { providers -> allProviders = providers }
                    .onFailure { error ->
                        // Non-fatal: the screen still works with just the "Collections" chip if
                        // this fails, but it shouldn't fail silently with no trace at all.
                        Log.w(TAG, "Failed to load watch providers, filter chips will be empty: $error")
                    }
            }
            val showsResult = tmdbRepository.getTrendingTvShows(null)
            providersDeferred.await()

            showsResult
                .onSuccess { shows ->
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = null,
                        collections = defaultCollections(),
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }

    /** Resolves one collection's shows via TMDB's genre- or network-discover query (whichever
     *  [DiscoverCollection] actually carries), the first time its row actually enters composition
     *  (DiscoverScreen's `CollectionRow`). A query that fails leaves the collection with an empty
     *  (not null) list, so the row hides rather than spinning forever; results are capped to keep
     *  the row a reasonable scroll length rather than a full 20-item TMDB page. */
    fun loadCollectionShows(collection: DiscoverCollection) {
        if (!requestedTitles.add(collection.title)) return

        viewModelScope.launch {
            val result = when {
                collection.genreId != null -> tmdbRepository.discoverByGenre(collection.genreId, collection.minVoteCount)
                collection.networkId != null -> tmdbRepository.discoverByNetwork(collection.networkId, collection.minVoteCount)
                else -> Result.Success(emptyList())
            }
            val shows = when (result) {
                is Result.Success -> result.data.take(COLLECTION_SHOW_LIMIT)
                is Result.Error -> emptyList()
            }
            _uiState.update { state ->
                if (state is DiscoverUiState.Success) {
                    state.copy(collections = state.collections.map {
                        if (it.title == collection.title) it.copy(shows = shows) else it
                    })
                } else state
            }
        }
    }

    fun selectProvider(providerId: Int?) {
        loadTrending(providerId, isClassic = false)
    }

    /** The "All" chip - same providerId=null trending query as "Collections", but reproduces the
     *  screen's pre-collections appearance (see DiscoverScreen's visibleCollections comment) for
     *  anyone who preferred that simpler view. */
    fun selectClassic() {
        loadTrending(providerId = null, isClassic = true)
    }

    private fun loadTrending(providerId: Int?, isClassic: Boolean) {
        loadTrendingJob?.cancel()
        loadTrendingJob = viewModelScope.launch {
            // Keep current success state if possible to avoid flickering while filtering
            val currentState = _uiState.value
            if (currentState !is DiscoverUiState.Success) {
                _uiState.value = DiscoverUiState.Loading
            } else {
                _uiState.update {
                    (it as DiscoverUiState.Success).copy(
                        selectedProviderId = providerId,
                        isClassicSelected = isClassic,
                    )
                }
            }

            tmdbRepository.getTrendingTvShows(providerId)
                .onSuccess { shows ->
                    // Read collections from the *latest* state, not a snapshot captured before this
                    // suspending call - a collection row can finish hydrating (loadCollectionShows())
                    // while this request is in flight, and _uiState.update applies against whatever the
                    // current value is, so that hydration survives instead of being clobbered by a stale
                    // pre-network snapshot (the collection's title would already be in requestedTitles by
                    // then, so a clobbered row could never retry and would spin forever).
                    val fallbackCollections = defaultCollections()
                    _uiState.update { state ->
                        val latestCollections = (state as? DiscoverUiState.Success)?.collections
                        if (latestCollections == null) {
                            // The Success state (and whatever collections it held) was lost - e.g. the
                            // state was Error or Loading at this point - so we're about to rebuild every
                            // collection from scratch with shows = null. Forget which titles were already
                            // requested, or a title that was requested against the now-discarded state
                            // could never be re-fetched and its row would spin forever (see the comment
                            // above and requestedTitles' own doc comment).
                            requestedTitles.clear()
                        }
                        DiscoverUiState.Success(
                            shows = shows,
                            providers = allProviders,
                            selectedProviderId = providerId,
                            collections = latestCollections ?: fallbackCollections,
                            isClassicSelected = isClassic,
                        )
                    }
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }
}
