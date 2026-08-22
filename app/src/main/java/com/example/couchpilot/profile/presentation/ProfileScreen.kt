package com.example.couchpilot.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Taste Profile") },
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
                is ProfileUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is ProfileUiState.Success -> {
                    if (state.isEmpty) {
                        Text(
                            text = "No taste data yet. Swipe on shows during onboarding, or " +
                                "up/downvote shows you view, and your profile will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else {
                        ProfileContent(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(state: ProfileUiState.Success) {
    val maxAbsWeight = state.genreAffinities.maxOf { abs(it.weight) }.coerceAtLeast(0.001)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "${state.totalSwipes} signals recorded - ${state.likedCount} liked, " +
                    "${state.dislikedCount} disliked. Genres you lean toward score higher in " +
                    "Tonight and Discover.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(
                text = "Genre affinity",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(state.genreAffinities) { affinity ->
            GenreAffinityRow(affinity = affinity, maxAbsWeight = maxAbsWeight)
        }
    }
}

@Composable
private fun GenreAffinityRow(affinity: GenreAffinity, maxAbsWeight: Double) {
    val isPositive = affinity.weight >= 0.0
    val fraction = (abs(affinity.weight) / maxAbsWeight).toFloat().coerceIn(0f, 1f)
    val barColor = if (isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(affinity.genreName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = (if (isPositive) "+" else "") + "%.1f".format(affinity.weight),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}
