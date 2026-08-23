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

    @Test
    fun `discoverByGenre maps the remote response to domain shows`() = runBlocking {
        coEvery { remoteDataSource.discoverTvByGenre(genreId = 18, minVoteCount = 300) } returns Result.Success(
            TrendingTvShowsResponseDto(
                results = listOf(
                    com.example.couchpilot.tmdb.data.dto.TvShowDto(
                        id = 61244, name = "Happy Valley", overview = "O", posterPath = null,
                        voteAverage = 8.4, firstAirDate = "2014", genreIds = listOf(18)
                    )
                )
            )
        )

        val result = repository.discoverByGenre(genreId = 18, minVoteCount = 300)

        assertTrue(result is Result.Success)
        assertEquals("Happy Valley", (result as Result.Success).data.first().name)
    }

    @Test
    fun `discoverByGenre does not apply taste-based ranking`() = runBlocking {
        // A non-empty preference vector would trigger rankShows() re-sorting if it were called -
        // discoverByGenre must not touch the scorer at all, since collections are meant to read
        // as "generally acclaimed," not personalized.
        coEvery { remoteDataSource.discoverTvByGenre(genreId = 99, minVoteCount = 100) } returns Result.Success(
            TrendingTvShowsResponseDto(results = emptyList())
        )

        repository.discoverByGenre(genreId = 99, minVoteCount = 100)

        coVerify(exactly = 0) { scorer.computePreferenceVector() }
    }
}
