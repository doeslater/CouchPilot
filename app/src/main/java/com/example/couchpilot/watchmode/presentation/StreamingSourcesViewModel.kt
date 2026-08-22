package com.example.couchpilot.watchmode.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.couchpilot.core.domain.onFailure
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.presentation.navigation.Route
import com.example.couchpilot.watchmode.domain.WatchmodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamingSourcesViewModel @Inject constructor(
    private val watchmodeRepository: WatchmodeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Route.StreamingSources>()
    
    private val _uiState = MutableStateFlow<StreamingSourcesUiState>(StreamingSourcesUiState.Loading)
    val uiState: StateFlow<StreamingSourcesUiState> = _uiState.asStateFlow()

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            _uiState.value = StreamingSourcesUiState.Loading
            watchmodeRepository.getStreamingSources(route.titleId)
                .onSuccess { sources ->
                    _uiState.value = StreamingSourcesUiState.Success(sources)
                }
                .onFailure { error ->
                    _uiState.value = StreamingSourcesUiState.Error(error.toString())
                }
        }
    }
}
