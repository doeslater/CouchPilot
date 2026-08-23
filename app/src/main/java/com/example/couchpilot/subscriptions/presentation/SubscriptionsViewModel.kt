package com.example.couchpilot.subscriptions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SubscriptionsUiState>(SubscriptionsUiState.Loading)
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = tmdbRepository.getWatchProviders()) {
                is Result.Success -> {
                    val subscribedIds = preferencesRepository.subscribedProviderIds.first()
                    _uiState.value = SubscriptionsUiState.Success(result.data, subscribedIds)
                }
                is Result.Error -> _uiState.value = SubscriptionsUiState.Error("Failed to load providers")
            }
        }
    }

    /** Ticks/unticks one provider and persists the full set immediately - no separate "Save"
     *  step, same immediate-write convention as ShowDetailViewModel's onToggleBookmark(). */
    fun toggleProvider(providerId: Int) {
        val state = _uiState.value
        if (state !is SubscriptionsUiState.Success) return

        val newIds = if (providerId in state.subscribedIds) {
            state.subscribedIds - providerId
        } else {
            state.subscribedIds + providerId
        }

        _uiState.update {
            if (it is SubscriptionsUiState.Success) it.copy(subscribedIds = newIds) else it
        }
        viewModelScope.launch {
            preferencesRepository.setSubscribedProviderIds(newIds)
        }
    }
}
