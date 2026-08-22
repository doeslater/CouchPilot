package com.example.couchpilot.watchmode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.watchmode.domain.WatchmodeRepository
import com.example.couchpilot.watchmode.domain.WatchmodeSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLEncoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_QUERY_LENGTH = 2

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val watchmodeRepository: WatchmodeRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _checkingResultId = MutableStateFlow<Int?>(null)
    /** The result currently being gated on a sources check after a tap - lets the row show a spinner. */
    val checkingResultId: StateFlow<Int?> = _checkingResultId.asStateFlow()

    private val _navigationEvents = Channel<SearchNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<SearchNavigationEvent> = _navigationEvents.receiveAsFlow()

    // Tracks the in-flight search so a second search-icon press (or IME "search" action) before
    // the first one lands cancels it, instead of letting a slower first response overwrite a
    // faster later one.
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        // Search no longer runs on every keystroke - typing just clears any previous results
        // once the field is emptied. The actual search only fires from onSearch(), i.e. the
        // search icon (or the keyboard's search action).
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState.Idle
        }
    }

    /** Runs the search - called when the user presses the search icon (or the IME search action). */
    fun onSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            when (val result = tmdbRepository.search(trimmed)) {
                is Result.Success -> {
                    val results = result.data.map { show ->
                        WatchmodeSearchResult(
                            id = 0, // Not available from TMDB search
                            name = show.name,
                            imageUrl = show.posterUrl,
                            isTvShow = show.mediaType == "tv",
                            tmdbId = show.id,
                            userRating = show.voteAverage,
                            overview = show.overview,
                            releaseDate = show.firstAirDate?.take(4)
                        )
                    }
                    _uiState.value = SearchUiState.Success(results)
                }
                is Result.Error -> _uiState.value = SearchUiState.Error(result.error.toString())
            }
        }
    }

    /**
     * A result isn't worth opening if it has nowhere to actually watch it - checked lazily here
     * (one Watchmode call, only on tap) rather than for every row up front, to keep a live-typed
     * search from multiplying Watchmode API calls by the result count on every keystroke.
     */
    fun onResultClick(result: WatchmodeSearchResult) {
        viewModelScope.launch {
            _checkingResultId.value = result.id
            // If we don't have the Watchmode ID yet (because search was via TMDB), we need to find it first.
            val watchmodeId = if (result.id == 0 && result.tmdbId != null) {
                findWatchmodeId(result.tmdbId, result.isTvShow)
            } else {
                result.id.toString()
            }

            if (watchmodeId == null) {
                _navigationEvents.send(SearchNavigationEvent.ToGoogle(googleSearchUrl(result.name)))
                _checkingResultId.value = null
                return@launch
            }

            watchmodeRepository.getStreamingSources(watchmodeId)
                .onSuccess { sources ->
                    val event = if (sources.isEmpty() || defaultDestination(result.copy(id = watchmodeId.toInt())) is SearchNavigationEvent.ToGoogle) {
                        SearchNavigationEvent.ToGoogle(googleSearchUrl(result.name))
                    } else {
                        defaultDestination(result.copy(id = watchmodeId.toInt()))
                    }
                    _navigationEvents.send(event)
                }
                .onFailure {
                    // Couldn't confirm availability either way - fail open to detail if we have
                    // the TMDB mapping, otherwise fallback to Google.
                    _navigationEvents.send(defaultDestination(result.copy(id = watchmodeId.toInt())))
                }
            _checkingResultId.value = null
        }
    }

    private suspend fun findWatchmodeId(tmdbId: Int, isTvShow: Boolean): String? {
        val type = if (isTvShow) "tmdb_tv_id" else "tmdb_movie_id"
        var foundId: String? = null
        watchmodeRepository.findTitleByExternalId(tmdbId.toString(), type)
            .onSuccess { result -> foundId = result?.id?.toString() }
        return foundId
    }

    private fun defaultDestination(result: WatchmodeSearchResult): SearchNavigationEvent {
        return if (result.tmdbId != null && result.isTvShow) {
            SearchNavigationEvent.ToShowDetail(result.tmdbId)
        } else {
            SearchNavigationEvent.ToGoogle(googleSearchUrl(result.name))
        }
    }

    private fun googleSearchUrl(showName: String): String {
        return AppEndpoint.WebSearch.GOOGLE + URLEncoder.encode(showName, "UTF-8")
    }
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<WatchmodeSearchResult>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface SearchNavigationEvent {
    data class ToShowDetail(val tmdbId: Int) : SearchNavigationEvent
    data class ToGoogle(val url: String) : SearchNavigationEvent
}
