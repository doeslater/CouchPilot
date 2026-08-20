package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.tvmaze.data.local.ScheduleDao
import com.example.couchpilot.tvmaze.domain.FreeviewChannels
import com.example.couchpilot.tvmaze.domain.ScheduleItem
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DefaultTvMazeRepository @Inject constructor(
    private val remoteDataSource: RetrofitTvMazeRemoteDataSource,
    private val scheduleDao: ScheduleDao
) : TvMazeRepository {
    override suspend fun getScheduleForDate(date: String): Result<List<ScheduleItem>, DataError> {
        val cached = scheduleDao.getScheduleForDate(date).first()
        if (cached.isNotEmpty()) {
            return Result.Success(rankByRating(cached.map { it.toScheduleItem() }))
        }

        val result = remoteDataSource.getSchedule(date = date)

        if (result is Result.Success) {
            val episodes = result.data
            val filtered = episodes.filter {
                FreeviewChannels.isFreeview(it.show.network?.name ?: it.show.webChannel?.name)
            }
            scheduleDao.insertScheduleItems(filtered.map { it.toEntity(date) })
        }

        return result.map { episodes ->
            rankByRating(
                episodes
                    .filter { FreeviewChannels.isFreeview(it.show.network?.name ?: it.show.webChannel?.name) }
                    .map { it.toScheduleItem() }
            )
        }
    }

    // Cold-start / pre-enrichment ordering only - no genre data exists at this point (TVmaze
    // doesn't supply TMDB-compatible genre IDs), so real personalization can't happen here.
    // TonightViewModel.enrichSchedule() re-ranks with RecommendationScorer once each item is
    // bridged to TMDB and has real genreIds.
    private fun rankByRating(items: List<ScheduleItem>): List<ScheduleItem> =
        items.sortedByDescending { it.rating ?: 0.0 }
}
