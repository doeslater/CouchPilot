package com.example.couchpilot.showdetail.presentation

import com.example.couchpilot.bookmarks.data.local.BookmarkDao
import com.example.couchpilot.bookmarks.data.local.BookmarkEntity
import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import com.example.couchpilot.showdetail.data.AppLauncher
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShowDetailViewModelTest {

    private val tmdbRepository: TmdbRepository = mockk()
    private val appLauncher: AppLauncher = mockk(relaxed = true)
    private val swipeEventDao: SwipeEventDao = mockk(relaxed = true)
    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val preferencesRepository: PreferencesRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val show = TvShow(
        id = 1,
        name = "Show 1",
        overview = "overview",
        posterUrl = null,
        voteAverage = 8.0,
        firstAirDate = "2024",
        genreIds = listOf(10, 20)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { tmdbRepository.getTvShowById(1) } returns Result.Success(show)
        coEvery { tmdbRepository.getWatchProvidersForShow(1) } returns Result.Success(emptyList())
        every { preferencesRepository.subscribedProviderIds } returns flowOf(emptySet())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(originProviderName: String? = null): ShowDetailViewModel {
        return ShowDetailViewModel(
            showId = 1, tmdbRepository, appLauncher, swipeEventDao, bookmarkDao,
            preferencesRepository, originProviderName
        )
    }

    @Test
    fun `loads show details into a success state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is ShowDetailUiState.Success)
        assertEquals(show, (state as ShowDetailUiState.Success).show)
    }

    @Test
    fun `onVote records an explicit full-weight signal and updates userVote`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()

        viewModel.onVote(true)
        runCurrent()

        coVerify {
            swipeEventDao.insertSwipeEvent(match<SwipeEventEntity> {
                it.showId == 1 && it.liked && it.weight == 1.0
            })
        }
        val state = viewModel.uiState.value as ShowDetailUiState.Success
        assertEquals(true, state.userVote)
    }

    @Test
    fun `a downvote is recorded as disliked at full weight`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()

        viewModel.onVote(false)
        runCurrent()

        coVerify {
            swipeEventDao.insertSwipeEvent(match<SwipeEventEntity> {
                it.showId == 1 && !it.liked && it.weight == 1.0
            })
        }
        val state = viewModel.uiState.value as ShowDetailUiState.Success
        assertEquals(false, state.userVote)
    }

    @Test
    fun `loads isBookmarked as true when the show already has a bookmark`() = runTest(testDispatcher) {
        coEvery { bookmarkDao.getBookmark(1) } returns BookmarkEntity(showId = 1)

        val viewModel = buildViewModel()
        runCurrent()

        val state = viewModel.uiState.value as ShowDetailUiState.Success
        assertTrue(state.isBookmarked)
    }

    @Test
    fun `onToggleBookmark inserts a bookmark and flips isBookmarked when not yet bookmarked`() =
        runTest(testDispatcher) {
            coEvery { bookmarkDao.getBookmark(1) } returns null
            val viewModel = buildViewModel()
            runCurrent()

            viewModel.onToggleBookmark()
            runCurrent()

            coVerify { bookmarkDao.insertBookmark(match<BookmarkEntity> { it.showId == 1 }) }
            val state = viewModel.uiState.value as ShowDetailUiState.Success
            assertTrue(state.isBookmarked)
        }

    @Test
    fun `onToggleBookmark deletes the bookmark and flips isBookmarked when already bookmarked`() =
        runTest(testDispatcher) {
            coEvery { bookmarkDao.getBookmark(1) } returns BookmarkEntity(showId = 1)
            val viewModel = buildViewModel()
            runCurrent()

            viewModel.onToggleBookmark()
            runCurrent()

            coVerify { bookmarkDao.deleteBookmark(1) }
            val state = viewModel.uiState.value as ShowDetailUiState.Success
            assertEquals(false, state.isBookmarked)
        }

    @Test
    fun `loads subscribedProviderIds from PreferencesRepository`() = runTest(testDispatcher) {
        every { preferencesRepository.subscribedProviderIds } returns flowOf(setOf(8, 9))

        val viewModel = buildViewModel()
        runCurrent()

        val state = viewModel.uiState.value as ShowDetailUiState.Success
        assertEquals(setOf(8, 9), state.subscribedProviderIds)
    }

    @Test
    fun `subscribedProviderIds defaults to empty when nothing is configured`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()

        val state = viewModel.uiState.value as ShowDetailUiState.Success
        assertTrue(state.subscribedProviderIds.isEmpty())
    }

    @Test
    fun `no dwell signal is recorded before the threshold elapses`() = runTest(testDispatcher) {
        buildViewModel()
        runCurrent()

        advanceTimeBy(DWELL_WEAK_SIGNAL_THRESHOLD_MS - 100)

        coVerify(exactly = 0) {
            swipeEventDao.insertSwipeEvent(match<SwipeEventEntity> { it.weight == DWELL_SIGNAL_WEIGHT })
        }
    }

    @Test
    fun `dwell past the threshold records a weak positive signal`() = runTest(testDispatcher) {
        buildViewModel()
        runCurrent()

        advanceTimeBy(DWELL_WEAK_SIGNAL_THRESHOLD_MS + 100)

        coVerify {
            swipeEventDao.insertSwipeEvent(match<SwipeEventEntity> {
                it.showId == 1 && it.liked && it.weight == DWELL_SIGNAL_WEIGHT
            })
        }
    }

    @Test
    fun `onProviderClick calls appLauncher with show name`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()
        val context = mockk<android.content.Context>()
        val provider = WatchProvider(id = 1, name = "Netflix", logoUrl = null, tmdbUrl = "https://tmdb.com/netflix")

        viewModel.onProviderClick(context, provider)

        verify { appLauncher.launchProviderApp(context, "Netflix", "Show 1", "https://tmdb.com/netflix") }
    }

    @Test
    fun `origin provider from a Discover chip is surfaced when AppLauncher has a website search for it`() =
        runTest(testDispatcher) {
            every { appLauncher.hasWebsiteSearch("BBC iPlayer") } returns true
            val viewModel = buildViewModel(originProviderName = "BBC iPlayer")
            runCurrent()

            val state = viewModel.uiState.value as ShowDetailUiState.Success
            assertEquals("BBC iPlayer", state.originProviderName)
        }

    @Test
    fun `origin provider is dropped when AppLauncher has no website search mapped for it`() =
        runTest(testDispatcher) {
            every { appLauncher.hasWebsiteSearch("Some Unmapped Provider") } returns false
            val viewModel = buildViewModel(originProviderName = "Some Unmapped Provider")
            runCurrent()

            val state = viewModel.uiState.value as ShowDetailUiState.Success
            assertEquals(null, state.originProviderName)
        }

    @Test
    fun `onOriginProviderClick opens the provider website with the show name`() = runTest(testDispatcher) {
        every { appLauncher.hasWebsiteSearch("BBC iPlayer") } returns true
        val viewModel = buildViewModel(originProviderName = "BBC iPlayer")
        runCurrent()
        val context = mockk<android.content.Context>()

        viewModel.onOriginProviderClick(context)

        verify { appLauncher.openProviderWebsite(context, "BBC iPlayer", "Show 1") }
    }

    @Test
    fun `onOriginProviderClick does nothing when there is no origin provider`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        runCurrent()
        val context = mockk<android.content.Context>()

        viewModel.onOriginProviderClick(context)

        verify(exactly = 0) { appLauncher.openProviderWebsite(any(), any(), any(), any()) }
    }
}
