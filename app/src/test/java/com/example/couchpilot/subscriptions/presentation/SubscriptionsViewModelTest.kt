package com.example.couchpilot.subscriptions.presentation

import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.WatchProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {

    private val tmdbRepository: TmdbRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk(relaxed = true)

    private val providers = listOf(
        WatchProvider(id = 1, name = "BBC iPlayer", logoUrl = null),
        WatchProvider(id = 2, name = "Netflix", logoUrl = null)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(providers)
    }

    @Test
    fun `loads providers and the current subscribed set`() {
        every { preferencesRepository.subscribedProviderIds } returns flowOf(setOf(1))

        val viewModel = SubscriptionsViewModel(tmdbRepository, preferencesRepository)

        val state = viewModel.uiState.value as SubscriptionsUiState.Success
        assertEquals(providers, state.providers)
        assertEquals(setOf(1), state.subscribedIds)
    }

    @Test
    fun `a failed provider load surfaces an error state`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Error(DataError.Network.UNKNOWN)
        every { preferencesRepository.subscribedProviderIds } returns flowOf(emptySet())

        val viewModel = SubscriptionsViewModel(tmdbRepository, preferencesRepository)

        assertTrue(viewModel.uiState.value is SubscriptionsUiState.Error)
    }

    @Test
    fun `toggleProvider adds an unsubscribed provider and persists the new set`() {
        every { preferencesRepository.subscribedProviderIds } returns flowOf(setOf(1))
        val viewModel = SubscriptionsViewModel(tmdbRepository, preferencesRepository)

        viewModel.toggleProvider(2)

        val state = viewModel.uiState.value as SubscriptionsUiState.Success
        assertEquals(setOf(1, 2), state.subscribedIds)
        coVerify { preferencesRepository.setSubscribedProviderIds(setOf(1, 2)) }
    }

    @Test
    fun `toggleProvider removes an already-subscribed provider and persists the new set`() {
        every { preferencesRepository.subscribedProviderIds } returns flowOf(setOf(1, 2))
        val viewModel = SubscriptionsViewModel(tmdbRepository, preferencesRepository)

        viewModel.toggleProvider(1)

        val state = viewModel.uiState.value as SubscriptionsUiState.Success
        assertEquals(setOf(2), state.subscribedIds)
        coVerify { preferencesRepository.setSubscribedProviderIds(setOf(2)) }
    }
}
