package com.example.couchpilot.tmdb.data

import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import com.example.couchpilot.tmdb.data.dto.WatchProvidersResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

    @GET(AppEndpoint.Tmdb.TRENDING_TV)
    suspend fun getTrendingTvShows(
        @Path("time_window") timeWindow: String = "day",
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>

    @GET(AppEndpoint.Tmdb.WATCH_PROVIDERS_TV)
    suspend fun getWatchProviders(
        @Query("watch_region") region: String,
        @Header("Authorization") authHeader: String
    ): Response<WatchProvidersResponseDto>

    @GET(AppEndpoint.Tmdb.DISCOVER_TV)
    suspend fun discoverTv(
        @Query("watch_region") region: String,
        @Query("with_watch_providers") providerIds: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_watch_monetization_types") monetization: String = "flatrate",
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>

    @GET(AppEndpoint.Tmdb.FIND_EXTERNAL)
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("external_source") externalSource: String = "imdb_id",
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.FindByIdResponseDto>

    @GET(AppEndpoint.Tmdb.TV_SHOW_WATCH_PROVIDERS)
    suspend fun getWatchProvidersForShow(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.ShowWatchProvidersResponseDto>

    @GET(AppEndpoint.Tmdb.TV_SHOW_DETAILS)
    suspend fun getTvShowDetails(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.TvShowDetailDto>

    @GET(AppEndpoint.Tmdb.TOP_RATED_TV)
    suspend fun getTopRatedTvShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>

    @GET(AppEndpoint.Tmdb.POPULAR_TV)
    suspend fun getPopularTvShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>
}
