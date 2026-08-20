package com.example.couchpilot.discover.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.couchpilot.tmdb.domain.TvShow

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DiscoverUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
            is DiscoverUiState.Error -> Text(
                text = "Couldn't load trending shows: ${state.message}",
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
            )
            is DiscoverUiState.Success -> TrendingGrid(shows = state.shows)
        }
    }
}

@Composable
private fun TrendingGrid(shows: List<TvShow>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(shows, key = { it.id }) { show -> ShowPoster(show) }
    }
}

@Composable
private fun ShowPoster(show: TvShow) {
    Column {
        AsyncImage(
            model = show.posterUrl,
            contentDescription = show.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = show.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
