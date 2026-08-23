package com.example.couchpilot.showdetail.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.couchpilot.tmdb.domain.TvShow
import com.example.couchpilot.tmdb.domain.WatchProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    onBack: () -> Unit,
    viewModel: ShowDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    if (state is ShowDetailUiState.Success) {
                        Text(state.show.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ShowDetailUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is ShowDetailUiState.Error -> Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                is ShowDetailUiState.Success -> {
                    val context = LocalContext.current
                    ShowDetailContent(
                        show = state.show,
                        providers = state.providers,
                        userVote = state.userVote,
                        originProviderName = state.originProviderName,
                        isBookmarked = state.isBookmarked,
                        onProviderClick = { viewModel.onProviderClick(context, it) },
                        onOriginProviderClick = { viewModel.onOriginProviderClick(context) },
                        onVote = viewModel::onVote,
                        onToggleBookmark = viewModel::onToggleBookmark
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowDetailContent(
    show: TvShow,
    providers: List<WatchProvider>,
    userVote: Boolean?,
    originProviderName: String?,
    isBookmarked: Boolean,
    onProviderClick: (WatchProvider) -> Unit,
    onOriginProviderClick: () -> Unit,
    onVote: (Boolean) -> Unit,
    onToggleBookmark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = show.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Backdrop style
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = show.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark this show",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Material's icon-core artifact doesn't include ThumbUp/ThumbDown outline
                // variants (only material-icons-extended does, which we're deliberately not
                // pulling in for two icons) - plain emoji glyphs instead, tinted to show the vote.
                IconButton(onClick = { onVote(true) }) {
                    Text(
                        text = "👍",
                        color = if (userVote == true) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onVote(false) }) {
                    Text(
                        text = "👎",
                        color = if (userVote == false) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = show.firstAirDate?.take(4) ?: "Unknown Date",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            // Only present when this screen was reached via a Discover provider chip - keeps
            // the user's path consistent with why they filtered in the first place, rather than
            // dropping them into the same generic list everyone else sees.
            if (originProviderName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search show on ",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = onOriginProviderClick
                    ) {
                        Text("$originProviderName")
                    }
                }
            }

            Text(
                text = "Rating: ${show.voteAverage}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = show.overview,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            if (providers.isNotEmpty()) {
                Text(
                    text = "Available on",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                providers.forEach { provider ->
                    ProviderRow(provider, onProviderClick)
                }
            }
            Spacer(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ProviderRow(
    provider: WatchProvider,
    onProviderClick: (WatchProvider) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = provider.logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Text(
            text = provider.name,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = { onProviderClick(provider) }) {
            Text("Open")
        }
    }
}
