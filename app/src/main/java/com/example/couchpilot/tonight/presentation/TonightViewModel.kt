package com.example.couchpilot.tonight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.recommendation.domain.RecommendationScorer
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
    private val recommendationScorer: RecommendationScorer,
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
                    val schedule = result.data
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
            val enrichedList = coroutineScope {
                schedule.map { item ->
                    async {
                        if (item.imdbId != null && item.posterUrl == null) {
                            when (val tmdbResult = tmdbRepository.getTvShowByImdbId(item.imdbId)) {
                                is Result.Success -> {
                                    val show = tmdbResult.data
                                    item.copy(
                                        posterUrl = show?.posterUrl,
                                        genreIds = show?.genreIds ?: emptyList(),
                                        tmdbId = show?.id,
                                    )
                                }
                                is Result.Error -> item
                            }
                        } else {
                            item
                        }
                    }
                }.awaitAll()
            }
            // Now that items have real genreIds (from the TMDB bridge above), re-rank with the
            // actual preference-based scorer instead of the repository's rating-only pre-sort.
            // score() returns 0.0 uniformly with no swipe signal yet, so rating stays the
            // tie-breaker/fallback for cold start.
            val userTaste = recommendationScorer.computePreferenceVector()
            val rankedList = enrichedList.sortedWith(
                compareByDescending<ScheduleItem> { recommendationScorer.score(it.genreIds, userTaste) }
                    .thenByDescending { it.rating ?: 0.0 }
            )
            _uiState.value = TonightUiState.Success(days, day, rankedList)
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
