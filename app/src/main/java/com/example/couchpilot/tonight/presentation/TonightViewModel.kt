package com.example.couchpilot.tonight.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** No TvMazeRepository to call yet — roadmap Phase 2 wires in the real UK schedule. */
@HiltViewModel
class TonightViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<TonightUiState>(TonightUiState.NotYetImplemented)
    val uiState: StateFlow<TonightUiState> = _uiState.asStateFlow()
}
