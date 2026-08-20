package com.example.couchpilot.presentation.navigation

import kotlinx.serialization.Serializable

/** Type-safe nav routes (kotlinx.serialization-backed, per androidx.navigation 2.8+). */
sealed interface Route {
    @Serializable
    data object Tonight : Route

    @Serializable
    data object Discover : Route

    @Serializable
    data object Onboarding : Route

    /** Destination arrives once ShowDetailScreen exists (roadmap Phase 6). */
    @Serializable
    data class ShowDetail(val id: Int) : Route
}
