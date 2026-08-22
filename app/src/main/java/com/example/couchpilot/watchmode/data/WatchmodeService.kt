package com.example.couchpilot.watchmode.data

import com.example.couchpilot.AppEndpoint
import com.example.couchpilot.watchmode.data.dto.WatchmodeSourceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WatchmodeService {
    @GET(AppEndpoint.Watchmode.TITLE_SOURCES)
    suspend fun getTitleSources(
        @Path("title_id") titleId: String,
        @Query("apiKey") apiKey: String,
        @Query("regions") regions: String = "GB"
    ): Response<List<WatchmodeSourceDto>>

    @GET(AppEndpoint.Watchmode.SEARCH)
    suspend fun searchTitles(
        @Query("apiKey") apiKey: String,
        @Query("search_field") searchField: String = "name",
        @Query("search_value") searchValue: String
    ): Response<com.example.couchpilot.watchmode.data.dto.WatchmodeSearchResponseDto>
}
