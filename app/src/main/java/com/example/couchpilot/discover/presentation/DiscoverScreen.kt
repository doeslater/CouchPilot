package com.example.couchpilot.discover.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.couchpilot.tmdb.domain.WatchProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    // originProviderName is null unless a specific chip (not "All") is selected - lets
    // ShowDetailScreen offer a CTA back to that same provider (see Route.ShowDetail's doc).
    onShowClick: (id: Int, originProviderName: String?) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Discover")
                        Text(
                            text = "Powered by TMDB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is DiscoverUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
                is DiscoverUiState.Error -> Text(
                    text = "Couldn't load trending shows: ${state.message}",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                is DiscoverUiState.Success -> Column(modifier = Modifier.fillMaxSize()) {
                    ProviderSelector(
                        providers = state.providers,
                        selectedId = state.selectedProviderId,
                        onProviderClick = { viewModel.selectProvider(it) }
                    )
                    val selectedProviderName = state.providers
                        .firstOrNull { it.id == state.selectedProviderId }
                        ?.name
                    TrendingGrid(
                        shows = state.shows,
                        onShowClick = { show -> onShowClick(show.id, selectedProviderName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSelector(
    providers: List<WatchProvider>,
    selectedId: Int?,
    onProviderClick: (Int?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedId == null,
                onClick = { onProviderClick(null) },
                label = { Text("All") }
            )
        }
        items(providers, key = { it.id }) { provider ->
            FilterChip(
                selected = selectedId == provider.id,
                onClick = { onProviderClick(provider.id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (provider.logoUrl != null) {
                            AsyncImage(
                                model = provider.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(provider.name)
                    }
                }
            )
        }
    }
}

@Composable
private fun TrendingGrid(
    shows: List<TvShow>,
    onShowClick: (TvShow) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(shows, key = { it.id }) { show ->
            ShowPoster(
                show = show,
                modifier = Modifier.clickable { onShowClick(show) }
            )
        }
    }
}

@Composable
private fun ShowPoster(
    show: TvShow,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
