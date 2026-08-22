package com.example.couchpilot.watchmode.data

import com.example.couchpilot.BuildConfig
import com.example.couchpilot.core.data.safeCall
import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.watchmode.data.dto.WatchmodeSearchResponseDto
import com.example.couchpilot.watchmode.data.dto.WatchmodeSearchResultDto
import com.example.couchpilot.watchmode.data.dto.WatchmodeSourceDto
import javax.inject.Inject

class RetrofitWatchmodeRemoteDataSource @Inject constructor(
    private val watchmodeService: WatchmodeService
) {
    suspend fun getTitleSources(titleId: String): Result<List<WatchmodeSourceDto>, DataError.Network> {
        return safeCall {
            watchmodeService.getTitleSources(
                titleId = titleId,
                apiKey = BuildConfig.WATCHMODE_API_KEY
            )
        }
    }

    suspend fun searchTitles(query: String): Result<WatchmodeSearchResponseDto, DataError.Network> {
        return safeCall {
            watchmodeService.autocompleteTitles(
                apiKey = BuildConfig.WATCHMODE_API_KEY,
                searchValue = query
            )
        }
    }

    suspend fun getTitleDetails(titleId: String): Result<WatchmodeSearchResultDto, DataError.Network> {
        return safeCall {
            watchmodeService.getTitleDetails(
                titleId = titleId,
                apiKey = BuildConfig.WATCHMODE_API_KEY
            )
        }
    }

    suspend fun findByExternalId(externalId: String, type: String): Result<WatchmodeSearchResponseDto, DataError.Network> {
        return safeCall {
            watchmodeService.findByExternalId(
                apiKey = BuildConfig.WATCHMODE_API_KEY,
                searchField = type,
                searchValue = externalId
            )
        }
    }
}
