package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.core.data.safeCall
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.Constants.DEFAULT_REGION
import com.example.couchpilot.tvmaze.data.dto.EpisodeDto
import javax.inject.Inject

class RetrofitTvMazeRemoteDataSource @Inject constructor(
    private val tvMazeService: TvMazeService
) {
    suspend fun getSchedule(country: String = DEFAULT_REGION, date: String): Result<List<EpisodeDto>, DataError.Network> {
        return safeCall {
            tvMazeService.getSchedule(country, date)
        }
    }
}
