package com.example.couchpilot.watchmode.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.watchmode.domain.WatchmodeRepository
import com.example.couchpilot.watchmode.domain.WatchmodeSource
import com.example.couchpilot.watchmode.domain.WatchmodeSearchResult
import javax.inject.Inject

class DefaultWatchmodeRepository @Inject constructor(
    private val remoteDataSource: RetrofitWatchmodeRemoteDataSource
) : WatchmodeRepository {
    override suspend fun getStreamingSources(titleId: String): Result<List<WatchmodeSource>, DataError> {
        return remoteDataSource.getTitleSources(titleId).map { dtos ->
            dtos.map { dto ->
                WatchmodeSource(
                    name = dto.name,
                    type = dto.type,
                    webUrl = dto.webUrl,
                    format = dto.format,
                    price = dto.price
                )
            }
        }
    }

    override suspend fun searchTitles(query: String): Result<List<WatchmodeSearchResult>, DataError> {
        return remoteDataSource.searchTitles(query).map { response ->
            val results = response.results ?: response.titleResults ?: emptyList()
            results.map { result ->
                WatchmodeSearchResult(
                    id = result.id,
                    name = result.name,
                    imageUrl = result.imageUrl,
                    isTvShow = (result.resultType ?: result.type)?.startsWith("tv", ignoreCase = true) == true,
                    tmdbId = result.tmdbId
                )
            }
                // Use a stable sort: prioritize TV shows (CouchPilot's primary focus) but
                // otherwise preserve the API's relevance ranking for better quality results.
                .sortedWith(
                    compareByDescending<WatchmodeSearchResult> { it.isTvShow }
                        .thenBy { 0 } // placeholder to keep it stable if needed, but sortedWith is stable
                )
        }
    }
}
