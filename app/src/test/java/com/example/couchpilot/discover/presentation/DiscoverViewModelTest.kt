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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

    // Shared fixture for every test that hydrates one genre-based collection - the specific
    // genre collection and the show TMDB resolves it to don't vary test to test, so tests just
    // reuse these rather than each hand-rolling their own copies.
    private val genreCollection = UK_CULTURE_COLLECTIONS.first { it.genreId != null }
    private val resolvedShow = TvShow(99, "Real Show", "O", null, 9.0, "2020", emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Default: every collection's genre/network discover query returns nothing, in case a
        // test below triggers loadCollectionShows() without caring about the result.
        coEvery { tmdbRepository.discoverByGenre(any(), any()) } returns Result.Success(emptyList())
        coEvery { tmdbRepository.discoverByNetwork(any(), any()) } returns Result.Success(emptyList())
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
    fun `collections are defined immediately with no shows loaded, and no query fires yet`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)

        val viewModel = buildViewModel()

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(UK_CULTURE_COLLECTIONS.size, state.collections.size)
        assertTrue(state.collections.all { it.shows == null })
        coVerify(exactly = 0) { tmdbRepository.discoverByGenre(any(), any()) }
        coVerify(exactly = 0) { tmdbRepository.discoverByNetwork(any(), any()) }
    }

    @Test
    fun `loadCollectionShows hydrates only the requested genre-based collection`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery {
            tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }

        viewModel.loadCollectionShows(discoverCollection)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        val hydrated = state.collections.first { it.title == genreCollection.title }
        assertEquals(listOf(resolvedShow), hydrated.shows)
        // every other collection is still unrequested
        assertTrue(state.collections.filter { it.title != genreCollection.title }.all { it.shows == null })
    }

    @Test
    fun `loadCollectionShows hydrates a network-based collection via discoverByNetwork, not discoverByGenre`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        val networkCollection = UK_CULTURE_COLLECTIONS.first { it.networkId != null }
        val resolvedShow = TvShow(9, "Real ITV Show", "O", null, 8.0, "2020", emptyList())
        coEvery {
            tmdbRepository.discoverByNetwork(networkCollection.networkId!!, networkCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == networkCollection.title }

        viewModel.loadCollectionShows(discoverCollection)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(listOf(resolvedShow), state.collections.first { it.title == networkCollection.title }.shows)
        coVerify(exactly = 0) { tmdbRepository.discoverByGenre(any(), any()) }
    }

    @Test
    fun `loadCollectionShows only fires the query once per collection`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }

        viewModel.loadCollectionShows(discoverCollection)
        viewModel.loadCollectionShows(discoverCollection)

        coVerify(exactly = 1) {
            tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
        }
    }

    @Test
    fun `a collection whose query fails ends up empty, not fatal to the screen`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery {
            tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
        } returns Result.Error(DataError.Network.SERVER_ERROR)
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }

        viewModel.loadCollectionShows(discoverCollection)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(emptyList<TvShow>(), state.collections.first { it.title == genreCollection.title }.shows)
    }

    @Test
    fun `collections survive a provider filter change, including any shows already loaded`() {
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } returns Result.Success(emptyList())
        coEvery {
            tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }
        viewModel.loadCollectionShows(discoverCollection)
        val collectionsBefore = (viewModel.uiState.value as DiscoverUiState.Success).collections

        viewModel.selectProvider(netflix.id)

        val state = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(collectionsBefore, state.collections)
        assertTrue(state.collections.any { it.shows?.isNotEmpty() == true })
    }

    @Test
    fun `a collection that finishes hydrating while a provider filter is still in flight is not clobbered`() {
        // UnconfinedTestDispatcher (used by every other test here) resolves suspending calls
        // synchronously, so it can never actually interleave two in-flight coroutines - this
        // regression needs real manual control over which suspends first, so it uses
        // StandardTestDispatcher + an explicit gate instead.
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
                coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
                val trendingGate = CompletableDeferred<Result<List<TvShow>, DataError>>()
                coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } coAnswers { trendingGate.await() }
                coEvery {
                    tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
                } returns Result.Success(listOf(resolvedShow))

                val viewModel = buildViewModel()
                dispatcher.scheduler.runCurrent()
                val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
                    .collections.first { it.title == genreCollection.title }

                // Provider filter tapped - getTrendingTvShows(netflix.id) suspends on trendingGate.
                viewModel.selectProvider(netflix.id)
                dispatcher.scheduler.runCurrent()

                // While that request is still in flight, the collection row's own query resolves.
                viewModel.loadCollectionShows(discoverCollection)
                dispatcher.scheduler.runCurrent()
                val hydratedWhileFilterPending = (viewModel.uiState.value as DiscoverUiState.Success)
                    .collections.first { it.title == genreCollection.title }
                assertEquals(listOf(resolvedShow), hydratedWhileFilterPending.shows)

                // Now let the provider-filtered trending query resolve.
                trendingGate.complete(Result.Success(emptyList()))
                dispatcher.scheduler.runCurrent()

                val finalCollection = (viewModel.uiState.value as DiscoverUiState.Success)
                    .collections.first { it.title == genreCollection.title }
                assertEquals(listOf(resolvedShow), finalCollection.shows)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `loadTrending rebuilds collections from scratch if the Success state was lost, and lets a stranded title retry`() {
        // Drives loadTrending's onSuccess into its state-not-Success fallback branch (line ~156):
        // hydrate one collection, then force the state to Error via one failed filter change, then
        // let a second filter change succeed while the state is still Error/Loading (not Success) -
        // reproducing "a collection's title was already requested against a Success state that then
        // got discarded" without needing to fight loadTrendingJob's own cancel-the-previous-job logic.
        coEvery { tmdbRepository.getWatchProviders() } returns Result.Success(listOf(netflix))
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(trendingShows)
        coEvery {
            tmdbRepository.discoverByGenre(genreCollection.genreId!!, genreCollection.minVoteCount)
        } returns Result.Success(listOf(resolvedShow))
        val viewModel = buildViewModel()
        val discoverCollection = (viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }
        viewModel.loadCollectionShows(discoverCollection)
        check((viewModel.uiState.value as DiscoverUiState.Success)
            .collections.first { it.title == genreCollection.title }.shows == listOf(resolvedShow))

        // A filter change whose trending request fails discards the Success state (and the
        // hydrated collection along with it).
        coEvery { tmdbRepository.getTrendingTvShows(netflix.id) } returns
            Result.Error(DataError.Network.SERVER_ERROR)
        viewModel.selectProvider(netflix.id)
        assertTrue(viewModel.uiState.value is DiscoverUiState.Error)

        // The next filter change succeeds while the current state is Error (not Success) - this
        // is exactly the fallback branch under test.
        coEvery { tmdbRepository.getTrendingTvShows(8080) } returns Result.Success(emptyList())
        viewModel.selectProvider(8080)

        val state = viewModel.uiState.value
        assertTrue(state is DiscoverUiState.Success)
        state as DiscoverUiState.Success
        assertEquals(UK_CULTURE_COLLECTIONS.size, state.collections.size)
        assertTrue(state.collections.all { it.shows == null })

        // Because the fallback also cleared requestedTitles, the collection that was already
        // "spent" against the now-discarded state can be requested again - it doesn't spin
        // forever with shows = null.
        val rebuiltCollection = state.collections.first { it.title == genreCollection.title }
        viewModel.loadCollectionShows(rebuiltCollection)

        val finalState = viewModel.uiState.value as DiscoverUiState.Success
        assertEquals(
            listOf(resolvedShow),
            finalState.collections.first { it.title == genreCollection.title }.shows,
        )
    }
}
