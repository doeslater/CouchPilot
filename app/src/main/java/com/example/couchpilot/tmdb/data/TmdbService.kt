package com.example.couchpilot.tmdb.data

import com.example.couchpilot.tmdb.data.dto.TrendingTvShowsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TmdbService {

    @GET("trending/tv/{time_window}")
    suspend fun getTrendingTvShows(
        @Path("time_window") timeWindow: String = "day",
        @Header("Authorization") authHeader: String
    ): Response<TrendingTvShowsResponseDto>
}
