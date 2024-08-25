package com.remotty.clientgui.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remotty.clientgui.R
import com.remotty.clientgui.ui.viewmodels.EpisodeDetailsViewModel
import com.remotty.clientgui.ui.viewmodels.EpisodesViewModel
import com.silverest.remotty.common.Chapter
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
    onClose: () -> Unit = {},
) {
    val nextEpisodeState by episodesViewModel.nextEpisode.collectAsState()
    val previousEpisodeState by episodesViewModel.previousEpisode.collectAsState()
    val episodeDetails by episodeDetailsViewModel.details.collectAsStateWithLifecycle()

    LaunchedEffect(showName) {
        episodeDetailsViewModel.fetchDetails()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isSmallScreen = maxHeight < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            episodeDetails.subs.takeIf { it.isNotEmpty() }?.let {
                SubtitleDropdown(it, episodeDetailsViewModel::updateSubtitle)
            }

            if (!isSmallScreen) Spacer(modifier = Modifier.height(20.dp)) else Spacer(modifier = Modifier.height(10.dp))


            Box(modifier = Modifier.fillMaxWidth().height(48.dp).align(Alignment.CenterHorizontally)) {
                IconButton(
                    onClick = onMute,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.mute),
                        contentDescription = "Mute",
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-5).dp, x = (-10).dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.shutdown),
                        contentDescription = "Close MPV",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }
            }

            if (!isSmallScreen) Spacer(modifier = Modifier.height(20.dp)) else Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val canGoBack = showName.isNotEmpty() && previousEpisodeState != null

                IconButton(onClick = onLast, enabled = canGoBack) {
                    Icon(
                        painter = painterResource(id = R.mipmap.skip_backward),
                        contentDescription = "Backward",
                        modifier = Modifier
                            .size(if (isSmallScreen) 40.dp else 48.dp)
                            .alpha(if (canGoBack) 1f else 0.7f)
                    )
                }

                IconButton(onClick = onBackward) {
                    Icon(
                        painter = painterResource(id = R.mipmap.seek_backward),
                        contentDescription = "Backward",
                        modifier = Modifier.size(if (isSmallScreen) 40.dp else 48.dp)
                    )
                }

                IconButton(onClick = onPausePlay) {
                    Icon(
                        painter = painterResource(id = R.mipmap.play),
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(if (isSmallScreen) 56.dp else 64.dp)
                    )
                }

                IconButton(onClick = onForward) {
                    Icon(
                        painter = painterResource(id = R.mipmap.seek_forward),
                        contentDescription = "Forward",
                        modifier = Modifier.size(if (isSmallScreen) 40.dp else 48.dp)
                    )
                }

                val canGoForward = showName.isNotEmpty() && nextEpisodeState != null
                IconButton(onClick = onNext, enabled = canGoForward) {
                    Icon(
                        painter = painterResource(id = R.mipmap.skip_forward),
                        contentDescription = "nextEpisode",
                        modifier = Modifier
                            .size(if (isSmallScreen) 40.dp else 48.dp)
                            .alpha(if (canGoForward) 1f else 0.7f)
                    )
                }
            }

            if (!isSmallScreen) Spacer(modifier = Modifier.height(20.dp)) else Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDecreaseVolume) {
                    Icon(
                        painter = painterResource(id = R.mipmap.decrease),
                        contentDescription = "Decrease Volume",
                        modifier = Modifier.size(if (isSmallScreen) 40.dp else 48.dp)
                    )
                }

                IconButton(onClick = onIncreaseVolume) {
                    Icon(
                        painter = painterResource(id = R.mipmap.increase),
                        contentDescription = "Increase Volume",
                        modifier = Modifier.size(if (isSmallScreen) 40.dp else 48.dp)
                    )
                }
            }

            if (!isSmallScreen) Spacer(modifier = Modifier.height(20.dp)) else Spacer(modifier = Modifier.height(10.dp))

            episodeDetails.chapters.takeIf { it.isNotEmpty() }?.let {
                ChapterControl(it, episodeDetailsViewModel::updateChapter, isSmallScreen)
            }
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
    onChapterSelected: (Int) -> Unit,
    isSmallScreen: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(if (isSmallScreen) 150.dp else 200.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 8.dp)
    ) {
        items(chapters) { chapter ->
            OutlinedButton(
                onClick = { onChapterSelected(chapter.index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 32.dp else 36.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(if (isSmallScreen) 1.dp else 2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = chapter.time,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}