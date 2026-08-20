package com.example.couchpilot.tonight.presentation

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.recommendation.domain.PreferenceVector
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tvmaze.domain.ScheduleItem
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TonightViewModelTest {

    private val tvMazeRepository: TvMazeRepository = mockk()
    private val tmdbRepository: TmdbRepository = mockk()
    private val recommendationScorer: RecommendationScorer = mockk()

    private val scheduleItem = ScheduleItem(
        id = 1,
        showId = 100,
        showName = "Show A",
        episodeName = null,
        airtime = "20:00",
        runtime = 60,
        channel = "BBC One",
        summary = null,
        imdbId = "tt1",
        rating = 7.0
    )

    private val enrichedShow = TvShow(
        id = 55,
        name = "Show A",
        overview = "",
        posterUrl = "poster-url",
        voteAverage = 7.0,
        firstAirDate = "2024",
        genreIds = listOf(1)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { recommendationScorer.computePreferenceVector() } returns PreferenceVector()
        every { recommendationScorer.score(any(), any()) } returns 0.0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectDay success enriches schedule via the TMDB bridge`() {
        coEvery { tvMazeRepository.getScheduleForDate(any()) } returns Result.Success(listOf(scheduleItem))
        coEvery { tmdbRepository.getTvShowByImdbId("tt1") } returns Result.Success(enrichedShow)

        val viewModel = TonightViewModel(tvMazeRepository, tmdbRepository, recommendationScorer)

        val state = viewModel.uiState.value
        assertTrue(state is TonightUiState.Success)
        val schedule = (state as TonightUiState.Success).schedule
        assertEquals(1, schedule.size)
        assertEquals(55, schedule[0].tmdbId)
        assertEquals("poster-url", schedule[0].posterUrl)
        assertEquals(listOf(1), schedule[0].genreIds)
    }

    @Test
    fun `an unresolved TMDB bridge leaves the schedule item title-only, not dropped`() {
        coEvery { tvMazeRepository.getScheduleForDate(any()) } returns Result.Success(listOf(scheduleItem))
        coEvery { tmdbRepository.getTvShowByImdbId("tt1") } returns Result.Success(null)

        val viewModel = TonightViewModel(tvMazeRepository, tmdbRepository, recommendationScorer)

        val schedule = (viewModel.uiState.value as TonightUiState.Success).schedule
        assertEquals(1, schedule.size)
        assertNull(schedule[0].tmdbId)
    }

    @Test
    fun `schedule fetch failure surfaces as an error state`() {
        coEvery { tvMazeRepository.getScheduleForDate(any()) } returns Result.Error(DataError.Network.NO_INTERNET)

        val viewModel = TonightViewModel(tvMazeRepository, tmdbRepository, recommendationScorer)

        assertTrue(viewModel.uiState.value is TonightUiState.Error)
    }

    @Test
    fun `selectDay requests the schedule for the newly selected day`() {
        coEvery { tvMazeRepository.getScheduleForDate(any()) } returns Result.Success(emptyList())

        val viewModel = TonightViewModel(tvMazeRepository, tmdbRepository, recommendationScorer)
        val days = (viewModel.uiState.value as TonightUiState.Success).days
        val tomorrow = days[1]

        viewModel.selectDay(tomorrow)

        coVerify { tvMazeRepository.getScheduleForDate(tomorrow.apiDate) }
        assertEquals(tomorrow, (viewModel.uiState.value as TonightUiState.Success).selectedDay)
    }
}
