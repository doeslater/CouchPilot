package com.example.couchpilot.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.domain.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadTrending()
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            tmdbRepository.getTrendingTvShows()
                .onSuccess { shows -> _uiState.value = DiscoverUiState.Success(shows) }
                // TODO(Phase 7 UiText mapping): a raw DataError.toString() is a placeholder,
                // not user-facing copy.
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }
}
