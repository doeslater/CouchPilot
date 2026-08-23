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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.runtime.LaunchedEffect
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
    // originProviderName is null unless a specific chip (not "Collections") is selected - lets
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
                        isClassicSelected = state.isClassicSelected,
                        onProviderClick = { viewModel.selectProvider(it) },
                        onClassicClick = { viewModel.selectClassic() }
                    )
                    val selectedProviderName = state.providers
                        .firstOrNull { it.id == state.selectedProviderId }
                        ?.name
                    // Collections are curated, provider-agnostic picks - showing them while a
                    // specific provider chip is selected buried the actually-filtered results
                    // below several rows of unchanged content, making the filter look broken (it
                    // wasn't - the grid below was filtering correctly the whole time, just out of
                    // view). Only show them for "Collections" so tapping a chip immediately
                    // surfaces the real filtered grid at the top of the screen. "All" also
                    // suppresses them deliberately - it's the explicit opt-out into the
                    // pre-collections screen.
                    val visibleCollections =
                        if (state.selectedProviderId == null && !state.isClassicSelected) state.collections
                        else emptyList()
                    TrendingGrid(
                        collections = visibleCollections,
                        shows = state.shows,
                        // Collection rows aren't provider-chip-scoped, so a tap from one never
                        // carries an originProviderName CTA - only the grid below does.
                        onCollectionShowClick = { show -> onShowClick(show.id, null) },
                        onShowClick = { show -> onShowClick(show.id, selectedProviderName) },
                        // Fires once a collection row actually scrolls into (or starts near) the
                        // viewport - see loadCollectionShows()'s doc for why this isn't fetched
                        // upfront for all 8 collections.
                        onCollectionVisible = viewModel::loadCollectionShows
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
    isClassicSelected: Boolean,
    onProviderClick: (Int?) -> Unit,
    onClassicClick: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedId == null && !isClassicSelected,
                onClick = { onProviderClick(null) },
                label = { Text("Collections") }
            )
        }
        item {
            // The original pre-collections Discover screen, for anyone who preferred that simpler
            // view - same unfiltered trending query as "Collections", the rows themselves just
            // deliberately hidden (see DiscoverScreen's visibleCollections comment).
            FilterChip(
                selected = isClassicSelected,
                onClick = onClassicClick,
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

// One grid, not a scrollable Column wrapping a separate LazyVerticalGrid - nesting two
// independently-scrollable vertical containers doesn't measure (a LazyVerticalGrid needs a
// bounded height, which a verticalScroll Column's infinite height can't give it). Collection
// rows are rendered as full-span grid items so they and the poster grid share one scroll.
@Composable
private fun TrendingGrid(
    collections: List<DiscoverCollection>,
    shows: List<TvShow>,
    onCollectionShowClick: (TvShow) -> Unit,
    onShowClick: (TvShow) -> Unit,
    onCollectionVisible: (DiscoverCollection) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        collections.forEach { collection ->
            // null = not loaded yet (show a loading row); non-null-and-empty = loaded but found
            // nothing (or the query failed) - hide the row entirely rather than showing a shelf
            // with nothing in it.
            if (collection.shows == null || collection.shows.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "collection-${collection.title}") {
                    CollectionRow(
                        collection = collection,
                        onShowClick = onCollectionShowClick,
                        onVisible = onCollectionVisible
                    )
                }
            }
        }
        items(shows, key = { it.id }) { show ->
            ShowPoster(
                show = show,
                modifier = Modifier.clickable { onShowClick(show) }
            )
        }
    }
}

@Composable
private fun CollectionRow(
    collection: DiscoverCollection,
    onShowClick: (TvShow) -> Unit,
    onVisible: (DiscoverCollection) -> Unit,
) {
    // Fires once this row actually enters composition - which, inside a LazyVerticalGrid, only
    // happens once it's visible or about to be (within Compose's own prefetch window), not for
    // every collection just because it's defined. Keyed on title (stable across recompositions of
    // the same collection, and unique whether it's genre- or network-based) rather than the
    // collection object itself, whose `shows` field will change once loaded and would otherwise be
    // treated as a "new" key.
    LaunchedEffect(collection.title) { onVisible(collection) }

    Column {
        Text(
            text = collection.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        if (collection.shows == null) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 16.dp).size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collection.shows, key = { it.id }) { show ->
                    ShowPoster(
                        show = show,
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onShowClick(show) }
                    )
                }
            }
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
