package com.remotty.clientgui.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.remotty.clientgui.network.ClientManager
import com.remotty.clientgui.ui.viewmodels.EpisodesViewModel
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.ScrollDirection
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun EpisodesListScreen(
    episodesViewModel: EpisodesViewModel,
    showName: String,
    navController: NavController,
    clientManager: ClientManager
) {
    val episodes by episodesViewModel.episodes.collectAsStateWithLifecycle()
    val watchedEpisodes by episodesViewModel.watchedEpisodes.collectAsStateWithLifecycle()
    val isLoading by episodesViewModel.isLoading.collectAsStateWithLifecycle()
    val canScrollUp by episodesViewModel.canScrollUp.collectAsStateWithLifecycle()
    val canScrollDown by episodesViewModel.canScrollDown.collectAsStateWithLifecycle()

    LaunchedEffect(showName) {
        episodesViewModel.clean()
        episodesViewModel.fetchWatchedEpisodes(showName)
        episodesViewModel.startFetchingEpisodes(showName)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (navController.currentDestination?.route?.contains("remoteControl") != true) {
                episodesViewModel.clean()
            }
        }
    }

    EpisodesList(
        episodes = episodes,
        watchedEpisodes = watchedEpisodes,
        onLoadMore = { direction ->
            episodesViewModel.fetchEpisodes(showName, direction)
        },
        onEpisodeClick = { selectedEpisode ->
            clientManager.sendSignal(Signal.PLAY, "$showName/${selectedEpisode.relativePath}")
            episodesViewModel.updateLastWatchedEpisode(showName, selectedEpisode.episode)
            episodesViewModel.updateNextAndLast(showName, selectedEpisode)
            episodesViewModel.updateWatchedStatus(showName, selectedEpisode.episode, true)
            navController.navigate("remoteControl/$showName")
        },
        onWatchedToggle = { episode ->
            episodesViewModel.updateWatchedStatus(showName, episode.episode, !episode.isWatched)
        },
        isLoading = isLoading,
        canScrollUp = canScrollUp,
        canScrollDown = canScrollDown
    )
}

@OptIn(FlowPreview::class)
@Composable
fun EpisodesList(
    episodes: List<EpisodeDescriptor>,
    watchedEpisodes: Set<Int>,
    onLoadMore: (ScrollDirection) -> Unit,
    onEpisodeClick: (EpisodeDescriptor) -> Unit,
    onWatchedToggle: (EpisodeDescriptor) -> Unit,
    isLoading: Boolean,
    canScrollUp: Boolean,
    canScrollDown: Boolean
) {
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, canScrollDown, canScrollUp, isLoading) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
            Triple(
                firstVisibleItem?.index,
                lastVisibleItem?.index,
                layoutInfo.totalItemsCount
            )
        }
            .distinctUntilChanged()
            .debounce(300)
            .collect { (firstVisible, lastVisible, total) ->
                if (lastVisible != null && total > 0 && lastVisible >= total - 4 && !isLoading && canScrollDown) {
                    onLoadMore(ScrollDirection.DOWN)
                } else if (firstVisible != null && firstVisible <= 2 && !isLoading && canScrollUp) {
                    onLoadMore(ScrollDirection.UP)
                }
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading && (episodes.isEmpty() || canScrollUp)) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LoadingIndicator()
            }
        }

        items(
            items = episodes,
            key = { "${it.episode}" }
        ) { episode ->
            key(episode.episode, episode.episode in watchedEpisodes) {
                EpisodeCard(
                    episode = episode.copy(isWatched = episode.episode in watchedEpisodes),
                    onClick = { onEpisodeClick(it) },
                    onWatchedToggle = { onWatchedToggle(it) }
                )
            }
        }

        if (isLoading && episodes.isNotEmpty() && canScrollDown) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun EpisodeCard(
    episode: EpisodeDescriptor,
    onClick: (EpisodeDescriptor) -> Unit,
    onWatchedToggle: (EpisodeDescriptor) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { onClick(episode) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            if (episode.thumbnail != null && BitmapFactory.decodeByteArray(
                    episode.thumbnail,
                    0,
                    episode.thumbnail!!.size
                ) != null
            ) {
                Image(
                    bitmap = BitmapFactory.decodeByteArray(episode.thumbnail, 0, episode.thumbnail!!.size)
                        .asImageBitmap(),
                    contentDescription = "Episode Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = episode.episodeLength,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Episode ${episode.episode}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(
                            color = if (episode.isWatched) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable {
                            onWatchedToggle(episode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (episode.isWatched) "Watched" else "Mark as watched",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}