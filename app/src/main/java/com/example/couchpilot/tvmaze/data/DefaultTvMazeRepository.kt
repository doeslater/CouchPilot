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
            return Result.Success(cached.map { it.toScheduleItem() })
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
            episodes
                .filter { FreeviewChannels.isFreeview(it.show.network?.name ?: it.show.webChannel?.name) }
                .map { it.toScheduleItem() }
        }
    }
}
