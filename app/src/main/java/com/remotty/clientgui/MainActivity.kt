package com.remotty.clientgui

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remotty.clientgui.data.*
import com.remotty.clientgui.network.ClientManager
import com.remotty.clientgui.ui.screens.ConnectionScreen
import com.remotty.clientgui.ui.screens.EpisodesListScreen
import com.remotty.clientgui.ui.screens.RemoteControlScreen
import com.remotty.clientgui.ui.screens.ShowsListScreen
import com.remotty.clientgui.ui.theme.ClientGuiTheme
import com.remotty.clientgui.ui.viewmodels.EpisodeDetailsViewModel
import com.remotty.clientgui.ui.viewmodels.EpisodesViewModel
import com.remotty.clientgui.ui.viewmodels.ShowsViewModel
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val connectionStatus = MutableStateFlow(false)

    private val clientManager: ClientManager by lazy {
        ClientManager(applicationContext)
    }

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private val episodesRepository: EpisodesRepository by lazy {
        EpisodesRepositoryImpl(clientManager)
    }


    private val episodesViewModel: EpisodesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EpisodesViewModel(
                    episodesRepository,
                    database.lastWatchedEpisodeDao(),
                    database.watchedEpisodeDao()
                ) as T
            }
        }
    }

    private val showsRepository: ShowsRepository by lazy {
        ShowsRepositoryImpl(clientManager)
    }

    private val showsViewModel: ShowsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShowsViewModel(
                    showsRepository
                ) as T
            }
        }
    }

    private val episodeDetailsRepository: EpisodeDetailsRepository by lazy {
        EpisodeDetailsRepositoryImpl(clientManager)
    }

    private val episodeDetailsViewModel: EpisodeDetailsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EpisodeDetailsViewModel(
                    episodeDetailsRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClientGuiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isConnected by connectionStatus.collectAsStateWithLifecycle()
                    val navController = rememberNavController()

                    MainScreen(
                        clientManager = clientManager,
                        showsViewModel = showsViewModel,
                        episodesViewModel = episodesViewModel,
                        episodeDetailsViewModel = episodeDetailsViewModel,
                        navController = navController,
                        isConnected = isConnected
                    )
                }
            }
        }

        lifecycleScope.launch {
            clientManager.connectionStatus.collect { status ->
                connectionStatus.value = status
            }
        }
    }

    override fun onDestroy() {
        clientManager.close()
        super.onDestroy()
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier,
               navController: NavHostController = rememberNavController(),
               isConnected: Boolean = false,
               showsViewModel: ShowsViewModel,
               episodesViewModel: EpisodesViewModel,
               episodeDetailsViewModel: EpisodeDetailsViewModel,
               clientManager: ClientManager) {
    Box(modifier = modifier) {
        NavHost(navController, startDestination = if (isConnected) "showsList" else "connectionScreen") {
            composable("connectionScreen") {
                ConnectionScreen(
                    lastIpAddress = clientManager.getLastIpAddress(),
                    onConnect = { ipAddress ->
                        clientManager.connect(ipAddress)
                        clientManager.saveLastIpAddress(ipAddress)
                        navController.navigate("showsList") {
                            popUpTo("connectionScreen") { inclusive = true }
                        }
                    },
                    onScan = { onScanComplete ->
                        clientManager.scanForServers(
                            onServerFound = { serverIp ->
                                onScanComplete(serverIp)
                            },
                            onScanComplete = {
                                onScanComplete(null)
                            }
                        )
                    },
                    onStopScan = {
                        clientManager.stopScan()
                    }
                )
            }
            composable("showsList") {
                ShowsListScreen(
                    showsViewModel = showsViewModel,
                    navController = navController,
                    clientManager = clientManager
                )
            }
            composable(
                "episodesList/{showName}",
                arguments = listOf(navArgument("showName") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val showName = backStackEntry.arguments?.getString("showName") ?: ""
                EpisodesListScreen(episodesViewModel, showName, navController, clientManager)
            }
            composable(
                "remoteControl/{showName}",
                arguments = listOf(
                    navArgument("showName") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val showName = backStackEntry.arguments?.getString("showName") ?: ""
                val nextEpisode by episodesViewModel.nextEpisode.collectAsStateWithLifecycle()
                val previousEpisode by episodesViewModel.previousEpisode.collectAsStateWithLifecycle()

                DisposableEffect(Unit) {
                    onDispose {
                        episodeDetailsViewModel.clean()
                    }
                }

                RemoteControlScreen(
                    showName = showName,
                    episodesViewModel = episodesViewModel,
                    episodeDetailsViewModel = episodeDetailsViewModel,
                    onIncreaseVolume = { clientManager.sendSignal(Signal.INCREASE) },
                    onDecreaseVolume = { clientManager.sendSignal(Signal.DECREASE) },
                    onMute = { clientManager.sendSignal(Signal.MUTE) },
                    onPausePlay = { clientManager.sendSignal(Signal.PLAY_OR_PAUSE) },
                    onForward = { clientManager.sendSignal(Signal.SEEK_FORWARD, "$it") },
                    onBackward = { clientManager.sendSignal(Signal.SEEK_BACKWARD, "$it") },
                    onNext = {
                        if (nextEpisode != null) {
                            clientManager.sendSignal(Signal.PLAY, "$showName/${nextEpisode!!.relativePath}")
                            episodesViewModel.updateNextAndLast(showName, nextEpisode!!)
                            episodeDetailsViewModel.fetchDetails()
                        }
                    },
                    onLast = {
                        if (previousEpisode != null) {
                            clientManager.sendSignal(Signal.PLAY, "$showName/${previousEpisode!!.relativePath}")
                            episodesViewModel.updateNextAndLast(showName, previousEpisode!!)
                            episodeDetailsViewModel.fetchDetails()
                        }
                    },
                    onClose = { clientManager.sendSignal(Signal.CLOSE)}
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    clientManager: ClientManager,
    showsViewModel: ShowsViewModel,
    episodesViewModel: EpisodesViewModel,
    episodeDetailsViewModel: EpisodeDetailsViewModel,
    navController: NavHostController,
    isConnected: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AppNavHost(
            navController = navController,
            clientManager = clientManager,
            showsViewModel = showsViewModel,
            episodesViewModel = episodesViewModel,
            episodeDetailsViewModel = episodeDetailsViewModel
        )

        val currentDestination by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(null)
        val isRemoteControlScreen = currentDestination?.destination?.route?.startsWith("remoteControl") == true

        if (isConnected && !isRemoteControlScreen) {
            RemoteControlFAB(
                onClick = {
                    navController.navigate("remoteControl/\"\"") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        LaunchedEffect(isConnected) {
            if (!isConnected) {
                navController.navigate("connectionScreen") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}

@Composable
fun RemoteControlFAB(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .size(56.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.remote),
                contentDescription = "Remote Control",
                modifier = Modifier.size(40.dp).rotate(45F)
            )
        }
    }
}