package com.example.couchpilot.watchmode.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToShowDetail: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val checkingResultId by viewModel.checkingResultId.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SearchNavigationEvent.ToShowDetail -> onNavigateToShowDetail(event.tmdbId)
                is SearchNavigationEvent.ToGoogle -> uriHandler.openUri(event.url)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Search")
                        Text(
                            text = "Search powered by TMDB • Sources by Watchmode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                label = { Text("Search titles") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            viewModel.onQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Box(modifier = Modifier.weight(1f).padding(top = 16.dp)) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> Text(
                        text = "Type a show name to find where to watch it.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is SearchUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is SearchUiState.Error -> Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is SearchUiState.Success -> {
                        if (state.results.isEmpty()) {
                            Text(
                                text = "No results found.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.results) { result ->
                                    ListItem(
                                        headlineContent = { Text(result.name) },
                                        supportingContent = {
                                            Column {
                                                val type = if (result.isTvShow) "TV Show" else "Movie"
                                                val meta = listOfNotNull(
                                                    type,
                                                    result.releaseDate,
                                                    result.userRating?.let { "★ $it" }
                                                ).joinToString(" • ")
                                                Text(meta)
                                                result.overview?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        },
                                        leadingContent = {
                                            AsyncImage(
                                                model = result.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                            )
                                        },
                                        trailingContent = {
                                            // Shown only while this row's tap is confirming it has
                                            // somewhere to actually watch it (see onResultClick).
                                            if (checkingResultId == result.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        modifier = Modifier.clickable { viewModel.onResultClick(result) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
