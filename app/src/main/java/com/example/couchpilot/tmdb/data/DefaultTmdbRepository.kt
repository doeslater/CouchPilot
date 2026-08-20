package com.example.couchpilot.tmdb.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.core.domain.onSuccess
import com.example.couchpilot.tmdb.data.local.TvShowDao
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DefaultTmdbRepository @Inject constructor(
    private val remoteDataSource: RetrofitTmdbRemoteDataSource,
    private val tvShowDao: TvShowDao
) : TmdbRepository {
    override suspend fun getTrendingTvShows(providerId: Int?): Result<List<TvShow>, DataError> {
        if (providerId == null) {
            val cached = tvShowDao.getAllTvShows().first()
            val isFresh = cached.isNotEmpty() && 
                (System.currentTimeMillis() - cached.first().lastUpdated < 24 * 60 * 60 * 1000)
            
            if (isFresh) {
                return Result.Success(cached.map { it.toTvShow() })
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

        return result.map { dto -> dto.results.map { it.toTvShow() } }
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

    private fun WatchProvider.priorityRank(): Int {
        return when {
            name.contains("BBC", ignoreCase = true) -> 0
            name == "ITVX" -> 1
            name.contains("Channel 4", ignoreCase = true) || name.contains("All 4", ignoreCase = true) -> 2
            name.contains("My5", ignoreCase = true) || name.contains("Channel 5", ignoreCase = true) -> 3
            name == "U" || name.contains("UKTV", ignoreCase = true) -> 4
            name.contains("Sky", ignoreCase = true) -> 5
            else -> 6
        }
    }
}
