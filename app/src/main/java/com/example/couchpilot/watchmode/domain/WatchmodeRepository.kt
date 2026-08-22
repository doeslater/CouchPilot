package com.example.couchpilot.watchmode.domain

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result

interface WatchmodeRepository {
    suspend fun getStreamingSources(titleId: String): Result<List<WatchmodeSource>, DataError>
    suspend fun searchTitles(query: String): Result<List<WatchmodeSearchResult>, DataError>
}
