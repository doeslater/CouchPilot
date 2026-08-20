package com.example.couchpilot.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.LocalDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val localDataManager: LocalDataManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Wipes Room + resets the onboarding flag. Resetting the flag is what sends the user back
     * through Onboarding - CouchPilotNavHost already reacts to hasCompletedOnboarding flipping
     * to false, so no navigation call is needed here.
     */
    fun clearLocalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            localDataManager.clearAllLocalData()
            _uiState.update { it.copy(isClearing = false, didClear = true) }
        }
    }

    fun onClearHandled() {
        _uiState.update { it.copy(didClear = false) }
    }
}
