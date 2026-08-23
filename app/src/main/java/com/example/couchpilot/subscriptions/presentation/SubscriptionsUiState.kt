package com.example.couchpilot.subscriptions.presentation

import com.example.couchpilot.tmdb.domain.WatchProvider

sealed interface SubscriptionsUiState {
    data object Loading : SubscriptionsUiState
    data class Success(
        val providers: List<WatchProvider>,
        val subscribedIds: Set<Int>
    ) : SubscriptionsUiState
    data class Error(val message: String) : SubscriptionsUiState
}
