package com.example.couchpilot.tonight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.recommendation.domain.rankedByRating
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tvmaze.domain.ScheduleItem
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val DAYS_AHEAD = 7

@HiltViewModel
class TonightViewModel @Inject constructor(
    private val tvMazeRepository: TvMazeRepository,
    private val tmdbRepository: TmdbRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TonightUiState>(TonightUiState.Loading)
    val uiState: StateFlow<TonightUiState> = _uiState.asStateFlow()

    private val days: List<DayOption> = buildDayOptions()

    // Tracks the in-flight day fetch so a rapid second day-chip tap can cancel the first one
    // instead of letting an out-of-order response overwrite the newer selection (same fix as
    // DiscoverViewModel's provider filter).
    private var loadJob: Job? = null

    init {
        selectDay(days.first())
    }

    fun selectDay(day: DayOption) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TonightUiState.Loading
            when (val result = tvMazeRepository.getScheduleForDate(day.apiDate)) {
                is Result.Success -> {
                    val schedule = result.data.rankedByRating()
                    _uiState.value = TonightUiState.Success(days, day, schedule)
                    enrichSchedule(day, schedule)
                }
                is Result.Error -> {
                    _uiState.value = TonightUiState.Error("Failed to load schedule")
                }
            }
        }
    }

    private fun enrichSchedule(day: DayOption, schedule: List<ScheduleItem>) {
        viewModelScope.launch {
            // These are independent TMDB lookups (not TVmaze - TVmaze's rate limit doesn't apply
            // here), so run them concurrently rather than one at a time.
            val enrichedList = coroutineScope {
                schedule.map { item ->
                    async {
                        if (item.imdbId != null && item.posterUrl == null) {
                            when (val tmdbResult = tmdbRepository.getTvShowByImdbId(item.imdbId)) {
                                is Result.Success -> item.copy(posterUrl = tmdbResult.data?.posterUrl)
                                is Result.Error -> item
                            }
                        } else {
                            item
                        }
                    }
                }.awaitAll()
            }.rankedByRating()
            _uiState.value = TonightUiState.Success(days, day, enrichedList)
        }
    }

    /** Today + the next [DAYS_AHEAD] - 1 days, each with an API-format date and a display label. */
    private fun buildDayOptions(): List<DayOption> {
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        return (0 until DAYS_AHEAD).map { offset ->
            if (offset > 0) calendar.add(Calendar.DAY_OF_YEAR, 1)
            val label = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> labelFormat.format(calendar.time)
            }
            DayOption(apiDate = apiFormat.format(calendar.time), label = label)
        }
    }
}
