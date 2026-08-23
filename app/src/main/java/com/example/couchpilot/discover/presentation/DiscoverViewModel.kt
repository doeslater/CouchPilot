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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading

            // Providers and trending shows are independent calls - fetch them concurrently
            // (not one after the other) so the spinner shows for max(latency), not the sum.
            val providersDeferred = async {
                tmdbRepository.getWatchProviders()
                    .onSuccess { providers -> allProviders = providers }
                    .onFailure { error ->
                        // Non-fatal: the screen still works with just the "Collections" chip if
                        // this fails, but it shouldn't fail silently with no trace at all.
                        Log.w(TAG, "Failed to load watch providers, filter chips will be empty: $error")
                    }
            }
            val collectionsDeferred = async { loadCollections() }
            val showsResult = tmdbRepository.getTrendingTvShows(null)
            providersDeferred.await()
            val collections = collectionsDeferred.await()

            showsResult
                .onSuccess { shows ->
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = null,
                        collections = collections,
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }

    /** Resolves every [UK_CULTURE_COLLECTIONS] entry via TMDB's genre discover query, concurrently
     *  across collections. A collection whose query fails is dropped (empty), not fatal to the
     *  other collections or the whole screen; results are capped to keep each row a reasonable
     *  scroll length rather than a full 20-item TMDB page. */
    private suspend fun loadCollections(): List<DiscoverCollection> = coroutineScope {
        UK_CULTURE_COLLECTIONS.map { collection ->
            async {
                val shows = when (
                    val result = tmdbRepository.discoverByGenre(collection.genreId, collection.minVoteCount)
                ) {
                    is Result.Success -> result.data.take(COLLECTION_SHOW_LIMIT)
                    is Result.Error -> emptyList()
                }
                DiscoverCollection(collection.title, shows)
            }
        }.awaitAll()
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
            val collections = (currentState as? DiscoverUiState.Success)?.collections ?: emptyList()
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
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = providerId,
                        collections = collections,
                        isClassicSelected = isClassic,
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }
}
