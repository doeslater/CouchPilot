package com.example.couchpilot.tmdb.data

import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.recommendation.domain.PreferenceVector
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.tmdb.data.local.TvShowEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultTmdbRepositoryTest {

    private lateinit var repository: DefaultTmdbRepository
    private val remoteDataSource: RetrofitTmdbRemoteDataSource = mockk()
    private val tvShowDao: TvShowDao = mockk(relaxed = true)
    private val scorer: RecommendationScorer = mockk(relaxed = true)

    @Before
    fun setup() {
        coEvery { scorer.computePreferenceVector() } returns PreferenceVector()
        repository = DefaultTmdbRepository(remoteDataSource, tvShowDao, scorer)
    }

    @Test
    fun `getTrendingTvShows returns cache if fresh`() = runBlocking {
        val now = System.currentTimeMillis()
        val cachedShows = listOf(
            TvShowEntity(1, "Cached", "Overview", null, 8.0, "2024", now)
        )
        coEvery { tvShowDao.getAllTvShows() } returns flowOf(cachedShows)

        val result = repository.getTrendingTvShows(null)

        assertTrue(result is Result.Success)
        assertEquals("Cached", (result as Result.Success).data[0].name)
        coVerify(exactly = 0) { remoteDataSource.getTrendingTvShows() }
    }

    @Test
    fun `getTrendingTvShows fetches from network if cache empty`() = runBlocking {
        coEvery { tvShowDao.getAllTvShows() } returns flowOf(emptyList())
        coEvery { remoteDataSource.getTrendingTvShows() } returns Result.Success(
            TrendingTvShowsResponseDto(results = emptyList())
        )

        repository.getTrendingTvShows(null)

        coVerify(exactly = 1) { remoteDataSource.getTrendingTvShows() }
        coVerify(exactly = 1) { tvShowDao.insertTvShows(any()) }
    }
}
