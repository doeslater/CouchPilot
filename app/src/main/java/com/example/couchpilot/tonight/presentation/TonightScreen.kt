package com.example.couchpilot.tonight.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun TonightScreen(
    viewModel: TonightViewModel = hiltViewModel(),
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "What's On Tonight is coming soon — the UK schedule isn't wired up yet.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
        )
    }
}
