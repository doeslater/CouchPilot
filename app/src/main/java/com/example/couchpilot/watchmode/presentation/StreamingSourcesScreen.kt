package com.example.couchpilot.watchmode.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.couchpilot.watchmode.domain.WatchmodeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingSourcesScreen(
    showName: String,
    onBack: () -> Unit,
    viewModel: StreamingSourcesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where to Watch: $showName") },
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
                is StreamingSourcesUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is StreamingSourcesUiState.Error -> Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                is StreamingSourcesUiState.Success -> {
                    if (state.sources.isEmpty()) {
                        Text(
                            text = "No streaming sources found for this region.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = innerPadding
                        ) {
                            items(state.sources) { source ->
                                SourceItem(
                                    source = source,
                                    onClick = { uriHandler.openUri(source.webUrl) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceItem(
    source: WatchmodeSource,
    onClick: () -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(source.name, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Text("${source.type.uppercase()} • ${source.format} ${source.price?.let { "• £$it" } ?: ""}")
        },
        trailingContent = {
            androidx.compose.material3.TextButton(onClick = onClick) {
                Text("WATCH")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
