package com.example.couchpilot.tmdb.data

import com.example.couchpilot.BuildConfig
import com.example.couchpilot.core.data.safeCall
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import javax.inject.Inject

class RetrofitTmdbRemoteDataSource @Inject constructor(
    private val tmdbService: TmdbService
) {
    suspend fun getTrendingTvShows(): Result<TrendingTvShowsResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.getTrendingTvShows(
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }
}
