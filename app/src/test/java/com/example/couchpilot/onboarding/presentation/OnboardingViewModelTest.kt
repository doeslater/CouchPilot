package com.example.couchpilot.onboarding.presentation

import com.example.couchpilot.core.data.PreferencesRepository
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private lateinit var viewModel: OnboardingViewModel
    private val tmdbRepository: TmdbRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk(relaxed = true)
    private val swipeEventDao: SwipeEventDao = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { tmdbRepository.getTrendingTvShows(null) } returns Result.Success(
            listOf(TvShow(1, "Show 1", "O1", null, 8.0, "2024", listOf(1)))
        )
        viewModel = OnboardingViewModel(tmdbRepository, preferencesRepository, swipeEventDao)
    }

    @Test
    fun `loadShows updates uiState with shows`() {
        val state = viewModel.uiState.value
        assertTrue(state is OnboardingUiState.Success)
        assertEquals(1, (state as OnboardingUiState.Success).shows.size)
    }

    @Test
    fun `onSwipe inserts event and moves to next index`() {
        val show = (viewModel.uiState.value as OnboardingUiState.Success).shows[0]
        viewModel.onSwipe(show, true)

        coVerify { swipeEventDao.insertSwipeEvent(any()) }
        val state = viewModel.uiState.value as OnboardingUiState.Success
        assertEquals(1, state.currentIndex)
        assertTrue(state.isFinished)
        coVerify { preferencesRepository.setOnboardingCompleted(true) }
    }

    @Test
    fun `skipOnboarding completes onboarding without recording any swipe event`() {
        viewModel.skipOnboarding()

        coVerify { preferencesRepository.setOnboardingCompleted(true) }
        coVerify(exactly = 0) { swipeEventDao.insertSwipeEvent(any()) }
    }
}
