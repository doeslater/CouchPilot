package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.Constants.DEFAULT_REGION
import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TvMazeService {

    @GET("schedule")
    suspend fun getSchedule(
        @Query("country") country: String = DEFAULT_REGION,
        @Query("date") date: String // YYYY-MM-DD
    ): Response<List<EpisodeDto>>
}
