package com.remotty.clientgui.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotty.clientgui.R
import com.remotty.clientgui.ui.viewmodels.EpisodeDetailsViewModel
import com.remotty.clientgui.ui.viewmodels.EpisodesViewModel
import com.silverest.remotty.common.Chapter
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.SubtitleTrack

@Composable
fun RemoteControlScreen(
    showName: String,
    episodesViewModel: EpisodesViewModel,
    episodeDetailsViewModel: EpisodeDetailsViewModel,
    onIncreaseVolume: () -> Unit = {},
    onDecreaseVolume: () -> Unit = {},
    onPausePlay: () -> Unit = {},
    onForward: () -> Unit = {},
    onBackward: () -> Unit = {},
    onNext: () -> Unit = {},
    onLast: () -> Unit = {},
    onMute: () -> Unit = {},
) {
    val nextEpisodeState by episodesViewModel.nextEpisode.collectAsState()
    val previousEpisodeState by episodesViewModel.previousEpisode.collectAsState()
    val episodeDetails by episodeDetailsViewModel.details.collectAsStateWithLifecycle()

    LaunchedEffect(showName) {
        episodeDetailsViewModel.fetchDetails()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        episodeDetails.subs.takeIf { it.isNotEmpty() }?.let {
            SubtitleDropdown(it, episodeDetailsViewModel::updateSubtitle)
        }

        Spacer(modifier = Modifier.height(32.dp))

        IconButton(onClick = onMute) {
            Icon(
                painter = painterResource(id = R.mipmap.mute),
                contentDescription = "Mute",
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val canGoBack = showName.isNotEmpty() && previousEpisodeState != null

            IconButton(
                onClick = onLast, enabled = canGoBack
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.skip_backward),
                    contentDescription = "Backward",
                    modifier = Modifier.size(48.dp).alpha(if (canGoBack) 1f else 0.7f)
                )
            }


            IconButton(onClick = onBackward) {
                Icon(
                    painter = painterResource(id = R.mipmap.seek_backward),
                    contentDescription = "Backward",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = onPausePlay) {
                Icon(
                    painter = painterResource(id = R.mipmap.play),
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(onClick = onForward) {
                Icon(
                    painter = painterResource(id = R.mipmap.seek_forward),
                    contentDescription = "Forward",
                    modifier = Modifier.size(48.dp)
                )
            }

            val canGoForward = showName.isNotEmpty() && nextEpisodeState != null
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    painter = painterResource(id = R.mipmap.skip_forward),
                    contentDescription = "nextEpisode",
                    modifier = Modifier.size(48.dp).alpha(if (canGoForward) 1f else 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDecreaseVolume) {
                Icon(
                    painter = painterResource(id = R.mipmap.decrease),
                    contentDescription = "Decrease Volume",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(onClick = onIncreaseVolume) {
                Icon(
                    painter = painterResource(id = R.mipmap.increase),
                    contentDescription = "Increase Volume",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        episodeDetails.chapters.takeIf { it.isNotEmpty() }?.let {
            ChapterControl(it, episodeDetailsViewModel::updateChapter)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleDropdown(
    subtitles: List<SubtitleTrack>,
    onSubtitleSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSubtitleIndex by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = subtitles.getOrNull(selectedSubtitleIndex)?.lang ?: "Select subtitle",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(4.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                subtitles.forEachIndexed { index, subtitle ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                subtitle.lang,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onClick = {
                            selectedSubtitleIndex = index
                            onSubtitleSelected(subtitle.id)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterControl(
    chapters: List<Chapter>,
    onChapterSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chapters) { chapter ->
            OutlinedButton(
                onClick = { onChapterSelected(chapter.index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = chapter.time,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
