package com.example.couchpilot.tvmaze.domain

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result

/**
 * TVmaze is the only data source behind this today (see [com.example.couchpilot.tvmaze.data.RetrofitTvMazeRemoteDataSource]),
 * same rationale as [com.example.couchpilot.tmdb.domain.TmdbRepository]: named/typed as a repository rather than a
 * data source because it's the seam where a local Room cache gets added later (ROADMAP.md Phase 3) without
 * presentation code needing to change.
 */
interface TvMazeRepository {
    suspend fun getScheduleForDate(date: String): Result<List<ScheduleItem>, DataError>
}
