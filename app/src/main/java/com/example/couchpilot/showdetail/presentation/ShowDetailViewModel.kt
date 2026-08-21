package com.example.couchpilot.showdetail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.couchpilot.core.domain.Result
import com.example.couchpilot.onboarding.data.local.SwipeEventDao
import com.example.couchpilot.onboarding.data.local.SwipeEventEntity
import com.example.couchpilot.presentation.navigation.Route
import com.example.couchpilot.showdetail.data.AppLauncher
import com.example.couchpilot.tmdb.domain.TmdbRepository
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Explicit signals (a real up/down decision) count fully; dwell time is only ever a hint that
// someone lingered on the screen, not that they liked what they saw - down-weighted accordingly.
internal const val DWELL_WEAK_SIGNAL_THRESHOLD_MS = 8_000L
private const val EXPLICIT_VOTE_WEIGHT = 1.0
internal const val DWELL_SIGNAL_WEIGHT = 0.3

@HiltViewModel
class ShowDetailViewModel internal constructor(
    private val showId: Int,
    private val tmdbRepository: TmdbRepository,
    private val appLauncher: AppLauncher,
    private val swipeEventDao: SwipeEventDao
) : ViewModel() {
    // savedStateHandle.toRoute() decodes route args via a real android.os.Bundle round-trip,
    // which needs Robolectric to run outside a device/emulator - not worth pulling in for one
    // Int arg. Delegating to the primary constructor above keeps Hilt's real injection path
    // exactly as before while letting tests pass showId directly, Bundle-free.
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        tmdbRepository: TmdbRepository,
        appLauncher: AppLauncher,
        swipeEventDao: SwipeEventDao
    ) : this(
        savedStateHandle.toRoute<Route.ShowDetail>().id,
        tmdbRepository,
        appLauncher,
        swipeEventDao
    )

    private val _uiState = MutableStateFlow<ShowDetailUiState>(ShowDetailUiState.Loading)
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    init {
        loadShowDetails()
    }

    fun onProviderClick(context: android.content.Context, provider: WatchProvider) {
        val state = _uiState.value
        val showName = if (state is ShowDetailUiState.Success) state.show.name else null
        appLauncher.launchProviderApp(context, provider.name, showName, provider.tmdbUrl)
    }

    /** Explicit up/downvote - same storage path (and DAO) as onboarding's swipe events. */
    fun onVote(liked: Boolean) {
        val state = _uiState.value
        if (state !is ShowDetailUiState.Success) return

        viewModelScope.launch {
            recordSignal(state.show, liked = liked, weight = EXPLICIT_VOTE_WEIGHT)
            _uiState.update {
                if (it is ShowDetailUiState.Success) it.copy(userVote = liked) else it
            }
        }
    }

    private fun loadShowDetails() {
        viewModelScope.launch {
            _uiState.value = ShowDetailUiState.Loading

            val showResult = tmdbRepository.getTvShowById(showId)
            val providerResult = tmdbRepository.getWatchProvidersForShow(showId)

            if (showResult is Result.Success) {
                val show = showResult.data
                if (show != null) {
                    val providers = if (providerResult is Result.Success) {
                        providerResult.data
                    } else emptyList()

                    _uiState.value = ShowDetailUiState.Success(show, providers)
                    startDwellTracking(show)
                } else {
                    _uiState.value = ShowDetailUiState.Error("Show not found")
                }
            } else {
                _uiState.value = ShowDetailUiState.Error("Failed to load details")
            }
        }
    }

    /**
     * Fires a weak positive signal once the user has stayed on this show's detail screen past
     * the threshold. Lives on viewModelScope rather than a Compose LaunchedEffect so it's plain
     * suspend logic (unit-testable with a TestDispatcher) and so it gets the cancellation behavior
     * for free: if the user backs out before the delay elapses, viewModelScope.onCleared()
     * cancels this coroutine and no signal is recorded at all - a quick glance and bounce isn't
     * interest either.
     */
    private fun startDwellTracking(show: TvShow) {
        viewModelScope.launch {
            delay(DWELL_WEAK_SIGNAL_THRESHOLD_MS)
            recordSignal(show, liked = true, weight = DWELL_SIGNAL_WEIGHT)
        }
    }

    private suspend fun recordSignal(show: TvShow, liked: Boolean, weight: Double) {
        swipeEventDao.insertSwipeEvent(
            SwipeEventEntity(
                showId = show.id,
                genreIds = show.genreIds.joinToString(","),
                liked = liked,
                weight = weight
            )
        )
    }
}
