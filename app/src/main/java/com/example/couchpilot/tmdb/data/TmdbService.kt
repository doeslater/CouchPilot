package com.example.couchpilot.tmdb.data

import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import com.example.couchpilot.tmdb.data.dto.WatchProvidersResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

    @GET("trending/tv/{time_window}")
    suspend fun getTrendingTvShows(
        @Path("time_window") timeWindow: String = "day",
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>

    @GET("watch/providers/tv")
    suspend fun getWatchProviders(
        @Query("watch_region") region: String,
        @Header("Authorization") authHeader: String
    ): Response<WatchProvidersResponseDto>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("watch_region") region: String,
        @Query("with_watch_providers") providerIds: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_watch_monetization_types") monetization: String = "flatrate",
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>

    @GET("find/{external_id}")
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("external_source") externalSource: String = "imdb_id",
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.FindByIdResponseDto>

    @GET("tv/{series_id}/watch/providers")
    suspend fun getWatchProvidersForShow(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.ShowWatchProvidersResponseDto>

    @GET("tv/{series_id}")
    suspend fun getTvShowDetails(
        @Path("series_id") seriesId: Int,
        @Header("Authorization") authHeader: String
    ): Response<com.example.couchpilot.tmdb.data.dto.TvShowDetailDto>
}
