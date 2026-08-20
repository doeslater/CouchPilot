package com.example.couchpilot.tmdb.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import javax.inject.Inject

class DefaultTmdbRepository @Inject constructor(
    private val remoteDataSource: RetrofitTmdbRemoteDataSource,
) : TmdbRepository {
    override suspend fun getTrendingTvShows(): Result<List<TvShow>, DataError> {
        return remoteDataSource.getTrendingTvShows()
            .map { dto -> dto.results.map { it.toTvShow() } }
    }
}
