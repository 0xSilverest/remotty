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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    val isLoading by episodesViewModel.isLoading.collectAsStateWithLifecycle()
    val canScrollUp by episodesViewModel.canScrollUp.collectAsStateWithLifecycle()
    val canScrollDown by episodesViewModel.canScrollDown.collectAsStateWithLifecycle()

    LaunchedEffect(showName) {
        episodesViewModel.clearEpisodes()
        episodesViewModel.startFetchingEpisodes(showName)
    }

    EpisodesList(
        episodes = episodes,
        onLoadMore = { direction ->
            episodesViewModel.fetchEpisodes(showName, direction)
        },
        onEpisodeClick = { selectedEpisode ->
            clientManager.sendSignal(Signal.PLAY, "$showName/${selectedEpisode.relativePath}")
            episodesViewModel.updateLastWatchedEpisode(showName, selectedEpisode.episode)
            episodesViewModel.updateNextAndLast(showName, selectedEpisode)
            navController.navigate("remoteControl/$showName")
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
    onLoadMore: (ScrollDirection) -> Unit,
    onEpisodeClick: (EpisodeDescriptor) -> Unit,
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
            key = { it.episode }
        ) { episode ->
            EpisodeCard(episode) { onEpisodeClick(it) }
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
fun EpisodeCard(item: EpisodeDescriptor, onClick: (EpisodeDescriptor) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { onClick(item) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            if (item.thumbnail != null) {
                Image(
                    bitmap = BitmapFactory.decodeByteArray(item.thumbnail, 0, item.thumbnail!!.size).asImageBitmap(),
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
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            start = Offset(Float.POSITIVE_INFINITY, 0f),
                            end = Offset(0f, Float.POSITIVE_INFINITY),
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = item.episodeLength,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Episode ${item.episode}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Watched indicator (assuming you have this information)
            if (item.isWatched) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Watched",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    tint = Color.White
                )
            }
        }
    }
}