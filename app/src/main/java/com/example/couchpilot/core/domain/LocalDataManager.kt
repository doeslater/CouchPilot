package com.example.couchpilot.core.domain

/**
 * Wipes all on-device app data - Room's cached shows/schedule/swipe history plus the
 * onboarding-completed flag. The concrete backing for general_idea.md's "no accounts,
 * everything stays on your device, and you can delete it all" pitch (Phase 7).
 */
interface LocalDataManager {
    suspend fun clearAllLocalData()
}
