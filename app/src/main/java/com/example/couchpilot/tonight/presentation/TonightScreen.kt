package com.example.couchpilot.tonight.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.couchpilot.tvmaze.domain.ScheduleItem

@Composable
fun TonightScreen(
    onShowClick: (Int) -> Unit,
    viewModel: TonightViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is TonightUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            is TonightUiState.Error -> Text(
                text = state.message,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            is TonightUiState.Success -> Column(modifier = Modifier.fillMaxSize()) {
                DaySelector(
                    days = state.days,
                    selectedDay = state.selectedDay,
                    onDayClick = viewModel::selectDay,
                )
                if (state.schedule.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "No Freeview shows matched for this day.",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        )
                    }
                } else {
                    ScheduleList(
                        schedule = state.schedule,
                        onShowClick = onShowClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    days: List<DayOption>,
    selectedDay: DayOption,
    onDayClick: (DayOption) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it.apiDate }) { day ->
            FilterChip(
                selected = day == selectedDay,
                onClick = { onDayClick(day) },
                label = { Text(day.label) },
            )
        }
    }
}

@Composable
private fun ScheduleList(
    schedule: List<ScheduleItem>,
    onShowClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(schedule, key = { it.id }) { item ->
            ScheduleRow(
                item = item,
                // Only clickable once enrichment has resolved a real TMDB id - not every show
                // matches the TVmaze-to-TMDB bridge (see TonightViewModel.enrichSchedule).
                modifier = Modifier.clickable(enabled = item.tmdbId != null) {
                    item.tmdbId?.let(onShowClick)
                }
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.airtime ?: "--:--",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(60.dp)
        )

        if (item.posterUrl != null) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 40.dp, height = 60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .padding(end = 12.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.showName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.episodeName != null) {
                Text(
                    text = item.episodeName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.channel ?: "Unknown Channel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
