package com.example.couchpilot.tonight.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tvmaze.domain.ScheduleItem
import com.example.couchpilot.tvmaze.domain.TvMazeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TonightViewModel @Inject constructor(
    private val tvMazeRepository: TvMazeRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<TonightUiState>(TonightUiState.Loading)
    val uiState: StateFlow<TonightUiState> = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.value = TonightUiState.Loading
            when (val result = tvMazeRepository.getTonightSchedule()) {
                is Result.Success -> {
                    val schedule = result.data
                    _uiState.value = TonightUiState.Success(schedule)
                    enrichSchedule(schedule)
                }
                is Result.Error -> {
                    _uiState.value = TonightUiState.Error("Failed to load schedule")
                }
            }
        }
    }

    private fun enrichSchedule(schedule: List<ScheduleItem>) {
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
            }
            _uiState.value = TonightUiState.Success(enrichedList)
        }
    }
}
