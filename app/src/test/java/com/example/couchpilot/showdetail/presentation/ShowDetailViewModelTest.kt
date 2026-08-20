package com.example.couchpilot.showdetail.presentation

import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import com.example.couchpilot.showdetail.data.AppLauncher
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): ShowDetailViewModel {
        return ShowDetailViewModel(showId = 1, tmdbRepository, appLauncher, swipeEventDao)
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
}
