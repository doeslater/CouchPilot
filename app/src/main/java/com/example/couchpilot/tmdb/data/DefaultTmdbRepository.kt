package com.example.couchpilot.tmdb.data

import com.example.couchpilot.AppConstants.DEFAULT_REGION
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.recommendation.domain.RecommendationScorer
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DefaultTmdbRepository @Inject constructor(
    private val remoteDataSource: RetrofitTmdbRemoteDataSource,
    private val tvShowDao: TvShowDao,
    private val recommendationScorer: RecommendationScorer
) : TmdbRepository {
    override suspend fun getTrendingTvShows(providerId: Int?): Result<List<TvShow>, DataError> {
        if (providerId == null) {
            val cached = tvShowDao.getAllTvShows().first()
            val isFresh = cached.isNotEmpty() &&
                (System.currentTimeMillis() - cached.first().lastUpdated < 24 * 60 * 60 * 1000)

            if (isFresh) {
                return Result.Success(rankShows(cached.map { it.toTvShow() }))
            }
        }

        val result = if (providerId == null) {
            remoteDataSource.getTrendingTvShows()
        } else {
            remoteDataSource.discoverTv(providerId = providerId)
        }

        if (result is Result.Success && providerId == null) {
            tvShowDao.insertTvShows(result.data.results.map { it.toEntity() })
        }

        return result.map { dto -> rankShows(dto.results.map { it.toTvShow() }) }
    }

    private suspend fun rankShows(shows: List<TvShow>): List<TvShow> {
        val userTaste = recommendationScorer.computePreferenceVector()
        if (userTaste.isEmpty()) return shows

        return shows.sortedByDescending { show ->
            recommendationScorer.score(show.genreIds, userTaste)
        }
    }

    override suspend fun getWatchProviders(): Result<List<WatchProvider>, DataError> {
        return remoteDataSource.getWatchProviders()
            .map { dto ->
                dto.results
                    .map { it.toWatchProvider() }
                    .sortedWith(compareBy<WatchProvider> { it.priorityRank() }.thenBy { it.name })
            }
    }

    override suspend fun getTvShowByImdbId(imdbId: String): Result<TvShow?, DataError> {
        return remoteDataSource.findByExternalId(imdbId).map { dto ->
            dto.tvResults.firstOrNull()?.toTvShow()
        }
    }

    override suspend fun getTvShowById(id: Int): Result<TvShow?, DataError> {
        val cached = tvShowDao.getTvShowById(id)
        if (cached != null) {
            return Result.Success(cached.toTvShow())
        }

        // Not every show reaches here via getTrendingTvShows() (e.g. ones bridged in from
        // Tonight/TVmaze via getTvShowByImdbId() are never inserted into tvShowDao), so a
        // cache miss isn't "not found" - fetch it directly and cache it for next time.
        return remoteDataSource.getTvShowDetails(id)
            .onSuccess { dto -> tvShowDao.insertTvShows(listOf(dto.toEntity())) }
            .map { dto -> dto.toTvShow() }
    }

    override suspend fun getWatchProvidersForShow(tvId: Int): Result<List<WatchProvider>, DataError> {
        return remoteDataSource.getWatchProvidersForShow(tvId).map { dto ->
            val regionData = dto.results[DEFAULT_REGION]
            val allProviders = (regionData?.flatrate ?: emptyList()) +
                               (regionData?.buy ?: emptyList()) +
                               (regionData?.rent ?: emptyList())

            allProviders
                .distinctBy { it.providerId }
                .map { it.toWatchProvider(regionData?.link) }
        }
    }

    override suspend fun search(query: String): Result<List<TvShow>, DataError> {
        return remoteDataSource.searchMulti(query).map { dto ->
            dto.results
                .filter { it.mediaType == "tv" || it.mediaType == "movie" }
                .map { it.toTvShow() }
        }
    }

    private fun WatchProvider.priorityRank(): Int {
        return when {
            name.contains("BBC", ignoreCase = true) -> 0
            name == "ITVX" -> 1
            name.contains("Channel 4", ignoreCase = true) || name.contains("All 4", ignoreCase = true) -> 2
            name.contains("My5", ignoreCase = true) || name.contains("Channel 5", ignoreCase = true) || name == "5" -> 3
            name == "U" || name.contains("UKTV", ignoreCase = true) -> 4
            name == "Sky Go" -> 5
            name.contains("arte", ignoreCase = true) -> 6
            name == "Netflix" -> 7
            name == "Apple TV" -> 8
            name == "Disney+" -> 9
            name == "HBO Max" -> 10
            name == "Amazon Prime" -> 11
            else -> 12
        }
    }
}
