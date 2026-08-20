package com.example.couchpilot.tmdb.data

import com.example.couchpilot.BuildConfig
import com.example.couchpilot.core.data.get
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import javax.inject.Inject

/**
 * The auth header is added per-request here, not on the shared [HttpClient] (see
 * [com.example.couchpilot.core.di.NetworkModule]) — it's TMDB's token and nothing else should get it.
 */
class KtorTmdbRemoteDataSource @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getTrendingTvShows(): Result<TrendingTvShowsResponseDto, DataError.Network> {
        return httpClient.get(url = TmdbRoutes.trendingTv()) {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
        }
    }
}
