package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.core.data.safeCall
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.AppConstants.DEFAULT_REGION
import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import com.example.couchpilot.tvmaze.data.dto.TvMazeSearchResponseDto
import com.example.couchpilot.tvmaze.data.dto.TvMazeShowDto
import javax.inject.Inject

class RetrofitTvMazeRemoteDataSource @Inject constructor(
    private val tvMazeService: TvMazeService
) {
    suspend fun getSchedule(country: String = DEFAULT_REGION, date: String): Result<List<EpisodeDto>, DataError.Network> {
        return safeCall {
            tvMazeService.getSchedule(country, date)
        }
    }

    suspend fun getShowByImdbId(imdbId: String): Result<TvMazeShowDto, DataError.Network> {
        return safeCall {
            tvMazeService.getShowByImdbId(imdbId)
        }
    }

    suspend fun searchShows(query: String): Result<List<TvMazeSearchResponseDto>, DataError.Network> {
        return safeCall {
            tvMazeService.searchShows(query)
        }
    }

    suspend fun getShowDetails(showId: Int): Result<TvMazeShowDto, DataError.Network> {
        return safeCall {
            tvMazeService.getShowDetails(showId)
        }
    }
}
