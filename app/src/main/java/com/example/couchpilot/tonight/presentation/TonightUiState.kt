package com.example.couchpilot.tonight.presentation

import com.example.couchpilot.tvmaze.domain.ScheduleItem

sealed interface TonightUiState {
    data object Loading : TonightUiState
    data class Success(val schedule: List<ScheduleItem>) : TonightUiState
    data class Error(val message: String) : TonightUiState
}
