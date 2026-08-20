package com.example.couchpilot.tvmaze.data

import com.example.couchpilot.core.domain.DataError
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.core.domain.map
import com.example.couchpilot.tvmaze.domain.FreeviewChannels
import com.example.couchpilot.tvmaze.domain.ScheduleItem
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class DefaultTvMazeRepository @Inject constructor(
    private val remoteDataSource: RetrofitTvMazeRemoteDataSource
) : TvMazeRepository {
    override suspend fun getTonightSchedule(): Result<List<ScheduleItem>, DataError> {
        // Locale.US, not getDefault(): this string is a TVmaze API query param, not user-facing
        // text - getDefault() would emit non-Latin digits on e.g. ar-SA/fa-IR and silently break
        // the request for those users.
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return remoteDataSource.getSchedule(date = today).map { episodes ->
            episodes
                .filter { FreeviewChannels.isFreeview(it.show.network?.name ?: it.show.webChannel?.name) }
                .map { it.toScheduleItem() }
        }
    }
}
