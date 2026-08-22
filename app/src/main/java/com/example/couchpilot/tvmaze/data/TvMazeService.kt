package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.AppConstants.DEFAULT_REGION
import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import com.example.couchpilot.tvmaze.data.dto.TvMazeSearchResponseDto
import com.example.couchpilot.tvmaze.data.dto.TvMazeShowDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvMazeService {

    @GET(AppEndpoint.TvMaze.SCHEDULE)
    suspend fun getSchedule(
        @Query("country") country: String = DEFAULT_REGION,
        @Query("date") date: String // YYYY-MM-DD
    ): Response<List<EpisodeDto>>

    @GET(AppEndpoint.TvMaze.SHOW_LOOKUP)
    suspend fun getShowByImdbId(
        @Query("imdb") imdbId: String
    ): Response<TvMazeShowDto>

    @GET(AppEndpoint.TvMaze.SHOW_SEARCH)
    suspend fun searchShows(
        @Query("q") query: String
    ): Response<List<TvMazeSearchResponseDto>>

    @GET(AppEndpoint.TvMaze.SHOW_DETAILS)
    suspend fun getShowDetails(
        @Path("id") showId: Int
    ): Response<TvMazeShowDto>
}
