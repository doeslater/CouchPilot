package com.example.couchpilot.onboarding.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.couchpilot.tmdb.domain.TvShow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    onShowInfo: (Int) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val swipeActions = remember { Channel<Boolean>(Channel.CONFLATED) }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        when (val state = uiState) {
            is OnboardingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is OnboardingUiState.Error -> Text(
                text = state.message,
                modifier = Modifier.align(Alignment.Center)
            )
            is OnboardingUiState.Success -> {
                if (state.isFinished) {
                    Text(
                        text = "Setting up your personal guide...",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Help us learn your taste",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Tap or swipe to decide",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        
                        Box(modifier = Modifier.weight(1f)) {
                            // Show the next card underneath if it exists
                            state.shows.getOrNull(state.currentIndex + 1)?.let { nextShow ->
                                ShowCard(show = nextShow, modifier = Modifier.graphicsLayer { alpha = 0.5f; scaleX = 0.9f; scaleY = 0.9f })
                            }
                            
                            state.currentShow?.let { currentShow ->
                                SwipeableCard(
                                    show = currentShow,
                                    onSwiped = { liked -> viewModel.onSwipe(currentShow, liked) },
                                    externalActions = swipeActions
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { scope.launch { swipeActions.send(false) } },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dislike", modifier = Modifier.size(32.dp))
                            }

                            FilledTonalIconButton(
                                onClick = { state.currentShow?.id?.let(onShowInfo) },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(24.dp))
                            }

                            FilledTonalIconButton(
                                onClick = { scope.launch { swipeActions.send(true) } },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFFE91E63).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFE91E63)
                                )
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(32.dp))
                            }
                        }

                        TextButton(
                            onClick = { viewModel.onSkipShow() },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text("Skip Show")
                        }
                    }
                }
            }
        }

        if (uiState is OnboardingUiState.Success && !(uiState as OnboardingUiState.Success).isFinished) {
            IconButton(
                onClick = { viewModel.skipOnboarding() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Skip onboarding")
            }
        }
    }
}

@Composable
fun SwipeableCard(
    show: TvShow,
    onSwiped: (Boolean) -> Unit,
    externalActions: Channel<Boolean>? = null
) {
    val offsetX = remember(show.id) { Animatable(0f) }

    LaunchedEffect(show.id) {
        externalActions?.let { actions ->
            for (liked in actions) {
                if (liked) {
                    offsetX.animateTo(1000f, tween(300))
                    onSwiped(true)
                } else {
                    offsetX.animateTo(-1000f, tween(300))
                    onSwiped(false)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = offsetX.value / 20f
            }
            .pointerInput(show.id) {
                val dragDeltas = Channel<Float>(Channel.UNLIMITED)
                coroutineScope {
                    launch {
                        for (delta in dragDeltas) {
                            offsetX.snapTo(offsetX.value + delta)
                        }
                    }
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX.value > 300f) {
                                launch {
                                    offsetX.animateTo(1000f, tween(300))
                                    onSwiped(true)
                                }
                            } else if (offsetX.value < -300f) {
                                launch {
                                    offsetX.animateTo(-1000f, tween(300))
                                    onSwiped(false)
                                }
                            } else {
                                launch {
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDeltas.trySend(dragAmount.x)
                        }
                    )
                    dragDeltas.close()
                }
            }
    ) {
        ShowCard(show = show)
    }
}

@Composable
fun ShowCard(
    show: TvShow,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            AsyncImage(
                model = show.posterUrl,
                contentDescription = show.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = show.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (show.firstAirDate != null) {
                    Text(
                        text = show.firstAirDate.take(4),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
