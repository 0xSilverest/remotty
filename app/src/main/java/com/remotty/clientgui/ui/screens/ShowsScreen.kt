package com.remotty.clientgui.ui.screens

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val shows by showsViewModel.shows.collectAsStateWithLifecycle()
    val isLoading by showsViewModel.isLoading.collectAsStateWithLifecycle()
    var isConnected by remember { mutableStateOf(false) }

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

    when {
        !isConnected -> {
            Text("Not connected")
        }
        isLoading -> {
            LoadingScreen()
        }
        shows.isEmpty() -> {
            Text("No shows available")
        }
        else -> {
            ShowsList(shows.toList(), navController)
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
        item.coverArt.let {
            val bitmap = remember {
                it?.let { img -> BitmapFactory.decodeByteArray(it, 0, img.size).asImageBitmap() }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "${item.name} Cover art",
                    modifier = Modifier
                        .height(280.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
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