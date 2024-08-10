package com.remotty.clientgui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remotty.clientgui.ui.theme.ClientGuiTheme
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val filesViewModel: ShowsViewModel by viewModels()
    private val episodesViewModel: EpisodesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientGuiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(this@MainActivity, showsViewModel = filesViewModel, episodesViewModel = episodesViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        ClientManager.close()
        super.onDestroy()
    }
}

@Composable
fun AppNavHost(lifecycleOwner: LifecycleOwner, showsViewModel: ShowsViewModel, episodesViewModel: EpisodesViewModel) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "showsList") {
        composable("showsList") {
            ShowsListScreen(lifecycleOwner, showsViewModel, navController)
        }
        composable(
            "episodesList/{showName}",
            arguments = listOf(navArgument("showName") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val showName = backStackEntry.arguments?.getString("showName") ?: ""
            EpisodesListScreen(lifecycleOwner, episodesViewModel, showName, navController)

            DisposableEffect(Unit) {
                onDispose {
                    if (navController.currentDestination?.route != "remoteControl") {
                        episodesViewModel.clearEpisodes()
                    }
                }
            }
        }
        composable("remoteControl") {
            RemoteControlScreen(
                onIncreaseVolume = {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.INCREASE) }
                },
                onDecreaseVolume = {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.DECREASE) }
                },
                onMute =
                {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.MUTE) }
                },
                onPausePlay = {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.PLAY_OR_PAUSE) }
                },
                onForward = {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.SEEK_FORWARD) }
                },
                onBackward = {
                    CoroutineScope(Dispatchers.IO).launch { ClientManager.sendSignal(Signal.SEEK_BACKWARD) }
                }
            )
        }
    }
}