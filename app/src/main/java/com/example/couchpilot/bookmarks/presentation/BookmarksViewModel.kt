package com.example.couchpilot.bookmarks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.bookmarks.data.local.BookmarkDao
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<BookmarksUiState>(BookmarksUiState.Loading)
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        // collectLatest re-runs the TMDB hydration below (and drops any still-in-flight one) every
        // time the bookmarks table changes, so bookmarking/unbookmarking from ShowDetailScreen is
        // reflected here live without this screen needing to be re-entered.
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collectLatest { bookmarks ->
                _uiState.value = BookmarksUiState.Success(hydrate(bookmarks.map { it.showId }))
            }
        }
    }

    /** Resolves bookmarked show ids to full [com.example.couchpilot.tmdb.domain.TvShow]s via the
     *  same cache-then-refresh repository ShowDetailScreen uses - concurrently, mirroring
     *  TonightViewModel's enrichSchedule(). A show whose lookup fails is silently dropped rather
     *  than surfacing an error for the whole list; [showIds]'s order (bookmark-recency, from the
     *  DAO's query) is preserved. */
    private suspend fun hydrate(showIds: List<Int>): List<TvShow> {
        if (showIds.isEmpty()) return emptyList()
        return coroutineScope {
            showIds.map { id ->
                async {
                    when (val result = tmdbRepository.getTvShowById(id)) {
                        is Result.Success -> result.data
                        is Result.Error -> null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }
}
