package com.example.couchpilot.watchmode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.watchmode.domain.WatchmodeRepository
import com.example.couchpilot.watchmode.domain.WatchmodeSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLEncoder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 300L
private const val MIN_QUERY_LENGTH = 2

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val watchmodeRepository: WatchmodeRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _checkingResultId = MutableStateFlow<Int?>(null)
    /** The result currently being gated on a sources check after a tap - lets the row show a spinner. */
    val checkingResultId: StateFlow<Int?> = _checkingResultId.asStateFlow()

    private val _navigationEvents = Channel<SearchNavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<SearchNavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        _query
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                // flatMapLatest cancels the previous inner flow (including an in-flight network
                // call) as soon as a newer query arrives - this is what stops a slow response for
                // an earlier keystroke from landing after, and clobbering, a faster later one.
                if (query.length < MIN_QUERY_LENGTH) {
                    flowOf<SearchUiState>(SearchUiState.Idle)
                } else {
                    flow<SearchUiState> {
                        emit(SearchUiState.Loading)
                        var next: SearchUiState = SearchUiState.Idle
                        watchmodeRepository.searchTitles(query)
                            .onSuccess { results -> next = SearchUiState.Success(results) }
                            .onFailure { error -> next = SearchUiState.Error(error.toString()) }
                        emit(next)
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _query.value = query
        // Clearing the field should feel instant, not wait out the debounce window.
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
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
            watchmodeRepository.getStreamingSources(result.id.toString())
                .onSuccess { sources ->
                    val event = if (sources.isEmpty()) {
                        SearchNavigationEvent.ToImdb(imdbSearchUrl(result.name))
                    } else {
                        defaultDestination(result)
                    }
                    _navigationEvents.send(event)
                }
                .onFailure {
                    // Couldn't confirm availability either way - fail open rather than swallow
                    // the tap, and let the destination screen surface its own error state.
                    _navigationEvents.send(defaultDestination(result))
                }
            _checkingResultId.value = null
        }
    }

    private fun defaultDestination(result: WatchmodeSearchResult): SearchNavigationEvent {
        return if (result.tmdbId != null && result.isTvShow) {
            SearchNavigationEvent.ToShowDetail(result.tmdbId)
        } else {
            SearchNavigationEvent.ToStreamingSources(result.id.toString(), result.name)
        }
    }

    private fun imdbSearchUrl(showName: String): String {
        return AppEndpoint.WebSearch.IMDB + URLEncoder.encode(showName, "UTF-8")
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
    data class ToStreamingSources(val titleId: String, val showName: String) : SearchNavigationEvent
    data class ToImdb(val url: String) : SearchNavigationEvent
}
