package com.example.couchpilot.discover.presentation

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.discover.domain.UK_CULTURE_COLLECTIONS
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
        // Default: every collection's genre discover query returns nothing, so existing tests
        // below (which don't care about collections) don't need to stub each genre individually.
        coEvery { tmdbRepository.discoverByGenre(any(), any()) } returns Result.Success(emptyList())
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

    @Test
    fun `selectClassic re-fetches the unfiltered trending query and marks classic mode selected`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)

        val viewModel = buildViewModel()
        viewModel.selectClassic()

        coVerify(exactly = 2) { tmdbRepository.getTrendingTvShows(null) } // init load + selectClassic
        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(null, state.selectedProviderId)
        assertTrue(state.isClassicSelected)
    }

    @Test
    fun `selectProvider clears classic mode`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } returns Result.Success(emptyList())

        val viewModel = buildViewModel()
        viewModel.selectClassic()
        viewModel.selectProvider(netflix.id)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertTrue(!state.isClassicSelected)
    }

    @Test
    fun `curated collections are hydrated via the genre discover query`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        val firstCollection = UK_CULTURE_COLLECTIONS.first()
        val resolvedShow = TvShow(99, "Real Show", "O", null, 9.0, "2020", emptyList())
        coEvery {
            tmdbRepository.discoverByGenre(firstCollection.genreId, firstCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value as DiscoverUiState.Success
        val hydrated = state.collections.first { it.title == firstCollection.title }
        assertEquals(listOf(resolvedShow), hydrated.shows)
    }

    @Test
    fun `a collection whose genre query fails is empty, not fatal to the screen`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        val firstCollection = UK_CULTURE_COLLECTIONS.first()
        coEvery {
            tmdbRepository.discoverByGenre(firstCollection.genreId, firstCollection.minVoteCount)
        } returns Result.Error(DataError.Network.SERVER_ERROR)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertTrue(state.collections.first { it.title == firstCollection.title }.shows.isEmpty())
    }

    @Test
    fun `collections survive a provider filter change`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } returns Result.Success(emptyList())
        val firstCollection = UK_CULTURE_COLLECTIONS.first()
        val resolvedShow = TvShow(99, "Real Show", "O", null, 9.0, "2020", emptyList())
        coEvery {
            tmdbRepository.discoverByGenre(firstCollection.genreId, firstCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))

        val viewModel = buildViewModel()
        val collectionsBefore = (viewModel.uiState.value as DiscoverUiState.Success).collections
        viewModel.selectProvider(netflix.id)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(collectionsBefore, state.collections)
        assertTrue(state.collections.any { it.shows.isNotEmpty() })
    }
}
