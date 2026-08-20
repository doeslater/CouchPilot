package com.example.couchpilot.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.WatchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var allProviders: List<WatchProvider> = emptyList()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState.Loading
            
            // Load providers first
            tmdbRepository.getWatchProviders().onSuccess { providers ->
                allProviders = providers
            }

            // Then load trending
            loadTrending(null)
        }
    }

    fun selectProvider(providerId: Int?) {
        loadTrending(providerId)
    }

    private fun loadTrending(providerId: Int?) {
        viewModelScope.launch {
            // Keep current success state if possible to avoid flickering while filtering
            val currentState = _uiState.value
            if (currentState !is DiscoverUiState.Success) {
                _uiState.value = DiscoverUiState.Loading
            } else {
                _uiState.update { 
                    (it as DiscoverUiState.Success).copy(selectedProviderId = providerId)
                }
            }

            tmdbRepository.getTrendingTvShows(providerId)
                .onSuccess { shows -> 
                    _uiState.value = DiscoverUiState.Success(
                        shows = shows,
                        providers = allProviders,
                        selectedProviderId = providerId
                    )
                }
                .onFailure { error -> _uiState.value = DiscoverUiState.Error(error.toString()) }
        }
    }
}
