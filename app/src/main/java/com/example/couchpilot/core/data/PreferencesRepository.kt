package com.example.couchpilot.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val SUBSCRIBED_PROVIDER_IDS = stringSetPreferencesKey("subscribed_provider_ids")

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    /** TMDB watch-provider ids the user has ticked as "I actually pay for this" - see
     *  `subscriptions/presentation/SubscriptionsScreen.kt`. Empty (the default, pre-any-tick)
     *  means "not configured yet," not "subscribed to nothing" - callers should treat an empty
     *  set as "don't judge," not "hide everything." DataStore has no native Set<Int>, so ids are
     *  stored as strings and parsed back; a value that fails to parse is dropped rather than
     *  crashing the read. */
    val subscribedProviderIds: Flow<Set<Int>> = context.dataStore.data
        .map { preferences ->
            preferences[SUBSCRIBED_PROVIDER_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun setSubscribedProviderIds(ids: Set<Int>) {
        context.dataStore.edit { preferences ->
            preferences[SUBSCRIBED_PROVIDER_IDS] = ids.map { it.toString() }.toSet()
        }
    }
}
