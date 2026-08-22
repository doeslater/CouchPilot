package com.example.couchpilot.tmdb.data

import com.example.couchpilot.BuildConfig
import com.example.couchpilot.core.data.safeCall
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.AppConstants.DEFAULT_REGION
import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import com.example.couchpilot.tmdb.data.dto.WatchProvidersResponseDto
import javax.inject.Inject

class RetrofitTmdbRemoteDataSource @Inject constructor(
    private val tmdbService: TmdbService
) {
    // CouchPilot is UK-only (per general_idea.md) — "GB" is the ISO-3166 region TMDB expects,
    // not "UK". Never default this back to "US".
    suspend fun getTrendingTvShows(): Result<TrendingTvShowsResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.getTrendingTvShows(
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun getWatchProviders(region: String = DEFAULT_REGION): Result<WatchProvidersResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.getWatchProviders(
                region = region,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun discoverTv(
        region: String = DEFAULT_REGION,
        providerId: Int
    ): Result<TrendingTvShowsResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.discoverTv(
                region = region,
                providerIds = providerId.toString(),
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun findByExternalId(externalId: String): Result<com.example.couchpilot.tmdb.data.dto.FindByIdResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.findByExternalId(
                externalId = externalId,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun getWatchProvidersForShow(tvId: Int): Result<com.example.couchpilot.tmdb.data.dto.ShowWatchProvidersResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.getWatchProvidersForShow(
                seriesId = tvId,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun getTvShowDetails(tvId: Int): Result<com.example.couchpilot.tmdb.data.dto.TvShowDetailDto, DataError.Network> {
        return safeCall {
            tmdbService.getTvShowDetails(
                seriesId = tvId,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun searchMulti(query: String): Result<com.example.couchpilot.tmdb.data.dto.TmdbSearchResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.searchMulti(
                query = query,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun searchTv(query: String): Result<com.example.couchpilot.tmdb.data.dto.TmdbSearchResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.searchTv(
                query = query,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }

    suspend fun searchMovie(query: String): Result<com.example.couchpilot.tmdb.data.dto.TmdbSearchResponseDto, DataError.Network> {
        return safeCall {
            tmdbService.searchMovie(
                query = query,
                authHeader = "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
            )
        }
    }
}
