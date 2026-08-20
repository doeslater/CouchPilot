package com.example.couchpilot.showdetail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.presentation.navigation.Route
import com.example.couchpilot.tmdb.domain.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {
    private val showId: Int = savedStateHandle.toRoute<Route.ShowDetail>().id

    private val _uiState = MutableStateFlow<ShowDetailUiState>(ShowDetailUiState.Loading)
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    init {
        loadShowDetails()
    }

    private fun loadShowDetails() {
        viewModelScope.launch {
            _uiState.value = ShowDetailUiState.Loading
            when (val result = tmdbRepository.getTvShowById(showId)) {
                is Result.Success -> {
                    val show = result.data
                    if (show != null) {
                        _uiState.value = ShowDetailUiState.Success(show)
                    } else {
                        _uiState.value = ShowDetailUiState.Error("Show not found")
                    }
                }
                is Result.Error -> {
                    _uiState.value = ShowDetailUiState.Error("Failed to load details")
                }
            }
        }
    }
}
