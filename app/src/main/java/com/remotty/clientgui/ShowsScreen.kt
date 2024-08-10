package com.remotty.clientgui

import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.silverest.remotty.common.ShowDescriptor

@Composable
fun ShowsListScreen(lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
                    showsViewModel: ShowsViewModel,
                    navController: NavController) {
    var showDescriptors by remember {
        mutableStateOf(emptyList<ShowDescriptor>())
    }

    LaunchedEffect(Unit) {
        showsViewModel.shows.observe(lifeCycleOwner) {
                shows -> showDescriptors = shows
        }
    }

    ShowsList(showDescriptors, navController)
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
        Text(
            text = item.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShowsList (showDescriptors: List<ShowDescriptor>, navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(showDescriptors) { showDescriptor ->
            ShowsCard(showDescriptor, navController)
        }
    }
}