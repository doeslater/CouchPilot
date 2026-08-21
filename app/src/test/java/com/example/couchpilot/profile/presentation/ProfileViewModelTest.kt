package com.example.couchpilot.profile.presentation

import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import com.example.couchpilot.recommendation.domain.PreferenceVector
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import io.mockk.coEvery
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
class ProfileViewModelTest {

    private val recommendationScorer: RecommendationScorer = mockk()
    private val swipeEventDao: SwipeEventDao = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `no swipe events yields an empty success state`() {
        coEvery { swipeEventDao.getAllSwipeEvents() } returns flowOf(emptyList())
        coEvery { recommendationScorer.computePreferenceVector() } returns PreferenceVector()

        val viewModel = ProfileViewModel(recommendationScorer, swipeEventDao)

        val state = viewModel.uiState.value as ProfileUiState.Success
        assertTrue(state.isEmpty)
        assertEquals(0, state.totalSwipes)
    }

    @Test
    fun `swipe events are summarized and genres are named and sorted by weight`() {
        val events = listOf(
            SwipeEventEntity(showId = 1, genreIds = "10765", liked = true),
            SwipeEventEntity(showId = 2, genreIds = "10764", liked = false),
            SwipeEventEntity(showId = 3, genreIds = "35", liked = true)
        )
        coEvery { swipeEventDao.getAllSwipeEvents() } returns flowOf(events)
        coEvery { recommendationScorer.computePreferenceVector() } returns PreferenceVector(
            mapOf(10765 to 1.0, 10764 to -1.0, 35 to 1.0)
        )

        val viewModel = ProfileViewModel(recommendationScorer, swipeEventDao)

        val state = viewModel.uiState.value as ProfileUiState.Success
        assertEquals(3, state.totalSwipes)
        assertEquals(2, state.likedCount)
        assertEquals(1, state.dislikedCount)
        assertEquals(listOf("Sci-Fi & Fantasy", "Comedy", "Reality"), state.genreAffinities.map { it.genreName })
        assertEquals(-1.0, state.genreAffinities.last().weight, 0.0)
    }
}
