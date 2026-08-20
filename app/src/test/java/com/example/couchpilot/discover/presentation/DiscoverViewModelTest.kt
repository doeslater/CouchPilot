package com.example.couchpilot.discover.presentation

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    private val tmdbRepository: TmdbRepository = mockk()

    private val netflix = WatchProvider(id = 8, name = "Netflix", logoUrl = null)
    private val trendingShows = listOf(
        TvShow(1, "Show 1", "O1", null, 8.0, "2024", listOf(1))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): DiscoverViewModel {
        return DiscoverViewModel(tmdbRepository)
    }

    @Test
    fun `initial load succeeds with providers and trending shows`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        state as DiscoverUiState.Success
        assertEquals(trendingShows, state.shows)
        assertEquals(listOf(netflix), state.providers)
        assertEquals(null, state.selectedProviderId)
    }

    @Test
    fun `provider load failure is non-fatal - trending shows still render`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Error(DataError.Network.NO_INTERNET)
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        state as DiscoverUiState.Success
        assertEquals(trendingShows, state.shows)
        assertEquals(emptyList<WatchProvider>(), state.providers)
    }

    @Test
    fun `trending load failure surfaces as an error state`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Error(DataError.Network.SERVER_ERROR)

        val viewModel = buildViewModel()

        assertTrue(viewModel.uiState.value is DiscoverUiState.Error)
    }

    @Test
    fun `selectProvider re-fetches trending shows filtered by that provider`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } returns Result.Success(emptyList())

        val viewModel = buildViewModel()
        viewModel.selectProvider(netflix.id)

        coVerify { tmdbRepository.getTrendingTvShows(netflix.id) }
        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(netflix.id, state.selectedProviderId)
        assertEquals(emptyList<TvShow>(), state.shows)
    }
}
