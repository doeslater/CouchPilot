package com.example.couchpilot.tmdb.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import javax.inject.Inject

class DefaultTmdbRepository @Inject constructor(
    private val remoteDataSource: RetrofitTmdbRemoteDataSource,
) : TmdbRepository {
    override suspend fun getTrendingTvShows(providerId: Int?): Result<List<TvShow>, DataError> {
        val result = if (providerId == null) {
            remoteDataSource.getTrendingTvShows()
        } else {
            remoteDataSource.discoverTv(providerId = providerId)
        }
        return result.map { dto -> dto.results.map { it.toTvShow() } }
    }

    override suspend fun getWatchProviders(): Result<List<WatchProvider>, DataError> {
        return remoteDataSource.getWatchProviders()
            .map { dto ->
                dto.results
                    .sortedBy { it.displayPriority }
                    .map { it.toWatchProvider() }
            }
    }
}
