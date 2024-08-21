package com.remotty.clientgui.ui.screens

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.remotty.clientgui.network.ClientManager
import com.remotty.clientgui.ui.viewmodels.ShowsViewModel
import com.silverest.remotty.common.ShowDescriptor

@Composable
fun ShowsListScreen(
                    showsViewModel: ShowsViewModel,
                    navController: NavController,
                    clientManager: ClientManager) {
    val filteredShows by showsViewModel.filteredShows.collectAsStateWithLifecycle()
    val isLoading by showsViewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by showsViewModel.searchQuery.collectAsStateWithLifecycle()
    var isConnected by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("ShowsListScreen", "Checking connection")
        isConnected = clientManager.isConnected()
        if (isConnected) {
            Log.d("ShowsListScreen", "Connected, requesting shows list")
            showsViewModel.requestShowsList()
        } else {
            Log.d("ShowsListScreen", "Not connected, navigating to connection screen")
            navController.navigate("connectionScreen") {
                popUpTo("showsList") { inclusive = true }
            }
        }
    }

    BackHandler {
        Log.d("ShowsListScreen", "Back pressed, cleaning up")
        showsViewModel.clean()
        clientManager.close()
        navController.navigate("connectionScreen") {
            popUpTo("showsList") { inclusive = true }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            showsViewModel.updateSearchQuery("")
            isSearchActive = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                this@Row.AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        "Anime List",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                this@Row.AnimatedVisibility(
                    visible = isSearchActive,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { showsViewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .height(56.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("Search anime...") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    )
                }
            }

            IconButton(
                onClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) showsViewModel.updateSearchQuery("")
                }
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "Close search" else "Open search"
                )
            }
        }

        when {
            !isConnected -> {
                Text("Not connected")
            }

            isLoading -> {
                LoadingScreen()
            }

            filteredShows.isEmpty() -> {
                if (searchQuery.isEmpty()) {
                    Text("No shows available")
                } else {
                    Text("No shows match your search")
                }
            }

            else -> {
                ShowsList(filteredShows.toList(), navController)
            }
        }
    }
}

@Composable
fun ShowsCard (item: ShowDescriptor, navController: NavController) {
    Log.d(TAG, "FileCard: $item")
    Column(
        modifier = Modifier
            .clickable {
                navController.navigate("episodesList/${item.name}")
            }
            .padding(8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        @Composable
        fun FallbackCoverArt(name: String) {
            Box(
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item.coverArt?.let { coverArt ->
            val bitmap = remember(coverArt) {
                runCatching {
                    BitmapFactory.decodeByteArray(coverArt, 0, coverArt.size)?.asImageBitmap()
                }.getOrNull()
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "${item.name} Cover art",
                    modifier = Modifier
                        .height(280.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                FallbackCoverArt(item.name)
            }
        } ?: FallbackCoverArt(item.name)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ShowsList(shows: List<ShowDescriptor>, navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(shows) { show ->
            ShowsCard(
                show,
                navController
            )
        }
    }
}