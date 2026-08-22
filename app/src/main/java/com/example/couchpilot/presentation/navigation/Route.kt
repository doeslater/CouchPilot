package com.example.couchpilot.presentation.navigation

import kotlinx.serialization.Serializable

/** Type-safe nav routes (kotlinx.serialization-backed, per androidx.navigation 2.8+). */
sealed interface Route {
    @Serializable
    data object Tonight : Route

    @Serializable
    data object Discover : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Onboarding : Route

    /**
     * Destination arrives once ShowDetailScreen exists (roadmap Phase 6).
     *
     * [originProviderName] is set only when reached by tapping a show from Discover's
     * provider-filtered grid (a specific chip was selected) - it carries that provider's
     * name (matching AppLauncher's map keys) so ShowDetailScreen can offer a CTA back to
     * that same provider, instead of just the generic "Available on" list. Null for any
     * other entry point (Tonight, Onboarding's info button, or Discover with "All" selected).
     */
    @Serializable
    data class ShowDetail(val id: Int, val originProviderName: String? = null) : Route

    /** Detail-style screen reached from Settings, not a bottom-nav tab. */
    @Serializable
    data object Profile : Route
}
