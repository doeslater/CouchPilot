package com.example.couchpilot.tonight.presentation

import com.example.couchpilot.tvmaze.domain.ScheduleItem

/**
 * @param apiDate yyyy-MM-dd, Locale.US - this is the TVmaze query param, not user-facing text.
 * @param label user-facing day label ("Today" / "Tomorrow" / "Fri 22 Aug"), device locale.
 */
data class DayOption(
    val apiDate: String,
    val label: String,
)

sealed interface TonightUiState {
    data object Loading : TonightUiState
    data class Success(
        val days: List<DayOption>,
        val selectedDay: DayOption,
        val schedule: List<ScheduleItem>,
    ) : TonightUiState
    data class Error(val message: String) : TonightUiState
}
