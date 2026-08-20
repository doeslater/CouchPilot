package com.example.couchpilot.showdetail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.presentation.navigation.Route
import com.example.couchpilot.showdetail.data.AppLauncher
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
    private val tmdbRepository: TmdbRepository,
    private val appLauncher: AppLauncher
) : ViewModel() {
    private val showId: Int = savedStateHandle.toRoute<Route.ShowDetail>().id

    private val _uiState = MutableStateFlow<ShowDetailUiState>(ShowDetailUiState.Loading)
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    init {
        loadShowDetails()
    }

    fun onProviderClick(context: android.content.Context, providerName: String) {
        appLauncher.launchProviderApp(context, providerName)
    }

    private fun loadShowDetails() {
        viewModelScope.launch {
            _uiState.value = ShowDetailUiState.Loading
            
            val showResult = tmdbRepository.getTvShowById(showId)
            val providerResult = tmdbRepository.getWatchProvidersForShow(showId)

            if (showResult is Result.Success) {
                val show = showResult.data
                if (show != null) {
                    val providers = if (providerResult is Result.Success) {
                        providerResult.data
                    } else emptyList()
                    
                    _uiState.value = ShowDetailUiState.Success(show, providers)
                } else {
                    _uiState.value = ShowDetailUiState.Error("Show not found")
                }
            } else {
                _uiState.value = ShowDetailUiState.Error("Failed to load details")
            }
        }
    }
}
