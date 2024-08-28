package com.remotty.clientgui.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.remotty.clientgui.network.ClientManager
import com.remotty.clientgui.ui.viewmodels.ShowsViewModel
import com.silverest.remotty.common.ShowDescriptor
import com.silverest.remotty.common.ShowFormat
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShowsListScreen(
    showsViewModel: ShowsViewModel,
    navController: NavController,
    clientManager: ClientManager
) {
    val filteredShows by showsViewModel.filteredShows.collectAsStateWithLifecycle()
    val isLoading by showsViewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by showsViewModel.searchQuery.collectAsStateWithLifecycle()
    var isConnected by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isConnected = clientManager.isConnected()
        if (isConnected) {
            showsViewModel.requestShowsList()
        } else {
            navController.navigate("connectionScreen") {
                popUpTo("showsList") { inclusive = true }
            }
        }
    }

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            showsViewModel.updateSearchQuery("")
        } else {
            showsViewModel.clean()
            clientManager.close()
            navController.navigate("connectionScreen") {
                popUpTo("showsList") { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            showsViewModel.updateSearchQuery("")
            isSearchActive = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val animatedPadding by animateDpAsState(
            targetValue = if (isSearchActive) 72.dp else 0.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow), label = ""
        )

        Box(modifier = Modifier.padding(top = animatedPadding)) {
            when {
                !isConnected -> {
                    Text("Not connected", modifier = Modifier.align(Alignment.Center))
                }

                isLoading -> {
                    LoadingScreen()
                }

                filteredShows.isEmpty() -> {
                    Text(
                        if (searchQuery.isEmpty()) "No shows available" else "No shows match your search",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    ShowsList(
                        shows = filteredShows.toList(),
                        clientManager = clientManager,
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { showsViewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search anime...") },
                trailingIcon = {
                    IconButton(onClick = {
                        isSearchActive = false
                        showsViewModel.updateSearchQuery("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        FloatingActionButton(
            onClick = { isSearchActive = !isSearchActive },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(2f)
        ) {
            Icon(
                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                contentDescription = if (isSearchActive) "Close search" else "Open search"
            )
        }
    }
}

@Composable
fun ShowsList(shows: List<ShowDescriptor>, navController: NavController, clientManager: ClientManager, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(shows) { show ->
            ShowCard(show = show, navController = navController, clientManager = clientManager)
        }
    }
}

@Composable
fun ShowCard(show: ShowDescriptor, navController: NavController, clientManager: ClientManager) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(show.coverArt) {
        isLoading = true
        imageBitmap = withContext(Dispatchers.IO) {
            show.coverArt?.let { coverArt ->
                runCatching {
                    BitmapFactory.decodeByteArray(coverArt, 0, coverArt.size)?.asImageBitmap()
                }.getOrNull()
            }
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable {
                if (show.format == ShowFormat.MOVIE) {
                    clientManager.sendSignal(Signal.PLAY_MOVIE, show.name)
                    navController.navigate("remoteControl/${show.name}")
                } else {
                    navController.navigate("episodesList/${show.name}")
                }
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> LoadingPlaceholder()
                imageBitmap != null -> {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "${show.name} Cover art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> ErrorPlaceholder(show.name)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            Text(
                text = show.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(0.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (show.format == ShowFormat.MOVIE) "Movie" else "TV",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ErrorPlaceholder(showName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = showName,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}