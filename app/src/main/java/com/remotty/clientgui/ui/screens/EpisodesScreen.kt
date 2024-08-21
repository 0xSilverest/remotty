package com.remotty.clientgui.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.remotty.clientgui.network.ClientManager
import com.remotty.clientgui.ui.viewmodels.EpisodesViewModel
import com.silverest.remotty.common.ScrollDirection
import com.silverest.remotty.common.EpisodeDescriptor
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
    val listState = rememberLazyGridState()

    LaunchedEffect(showName) {
        episodesViewModel.clearEpisodes()
        episodesViewModel.startFetchingEpisodes(showName)
    }

    EpisodesList(
        episodes = episodes,
        gridState = listState,
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
    gridState: LazyGridState,
    onLoadMore: (ScrollDirection) -> Unit,
    onEpisodeClick: (EpisodeDescriptor) -> Unit,
    isLoading: Boolean,
    canScrollUp: Boolean,
    canScrollDown: Boolean
) {

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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(8.dp)
    ) {
        if (isLoading && (episodes.isEmpty() || canScrollUp)) {
            item(span = { GridItemSpan(2) }) {
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
            item(span = { GridItemSpan(2) }) {
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
    Column(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(16f / 9f) // Maintain aspect ratio
            .clickable { onClick(item) }
    ) {
        @Composable
        fun FallbackBox() {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        if (item.thumbnail != null) {
            BitmapFactory.decodeByteArray(item.thumbnail, 0, item.thumbnail!!.size)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Episode Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: FallbackBox()
        } else {
            FallbackBox()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ep ${item.episode}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.episodeLength,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}