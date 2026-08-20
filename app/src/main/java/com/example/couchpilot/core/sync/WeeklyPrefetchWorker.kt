package com.example.couchpilot.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.time.LocalDate

import kotlinx.coroutines.coroutineScope

@HiltWorker
class WeeklyPrefetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tmdbRepository: TmdbRepository,
    private val tvMazeRepository: TvMazeRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Prefetch trending (global)
            val trendingJob = async { tmdbRepository.getTrendingTvShows(null) }
            
            // Prefetch next 7 days of schedule
            val calendar = java.util.Calendar.getInstance()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            
            val scheduleJobs = (0..6).map { 
                val dateStr = format.format(calendar.time)
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                async { tvMazeRepository.getScheduleForDate(dateStr) }
            }
            
            trendingJob.await()
            scheduleJobs.awaitAll()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
