package com.remotty.clientgui

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EpisodesListScreen(lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
                       episodesViewModel: EpisodesViewModel,
                       showName: String,
                       navController: NavController,) {
    var episodeDescriptors by remember {
        mutableStateOf(emptyList<EpisodeDescriptor>())
    }

    LaunchedEffect(showName) {
        if (episodesViewModel.episodes.value.isNullOrEmpty()) {
            episodesViewModel.fetchEpisodes(showName)
        }
    }

    episodesViewModel.episodes.observe(lifeCycleOwner) { episodes ->
        episodeDescriptors = episodes
    }

    EpisodesList(episodeDescriptors) { episodeDescriptor ->
        CoroutineScope(Dispatchers.IO).launch {
            ClientManager.sendSignal(Signal.PLAY, showName + "/" + episodeDescriptor.relativePath)
        }
        navController.navigate("remoteControl")
    }
}


@Composable
fun EpisodeCard (item: EpisodeDescriptor, param: (EpisodeDescriptor) -> Unit) {
    Log.d(TAG, "EpisodeCard: $item")
    Column(
        modifier = Modifier
            .clickable {
                param(item)
            }
            .padding(8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row {
            if (item.thumbnail != null) {
                Image(
                    bitmap = BitmapFactory.decodeByteArray(item.thumbnail, 0, item.thumbnail!!.size).asImageBitmap(),
                    contentDescription = "Episode Thumbnail",
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Ep ${item.episode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.episodeLength,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EpisodesList (episodesDescriptor: List<EpisodeDescriptor>, param: (EpisodeDescriptor) -> Unit) {
    LazyColumn {
        items(episodesDescriptor) { showDescriptor ->
            EpisodeCard(showDescriptor, param)
        }
    }
}