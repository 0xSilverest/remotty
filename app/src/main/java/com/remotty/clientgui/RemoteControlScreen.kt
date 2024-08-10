package com.remotty.clientgui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun RemoteControlScreen(
    onIncreaseVolume: () -> Unit = {},
    onDecreaseVolume: () -> Unit = {},
    onPausePlay: () -> Unit = {},
    onForward: () -> Unit = {},
    onBackward: () -> Unit = {},
    onNext: () -> Unit = {},
    onLast: () -> Unit = {},
    onMute: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            IconButton(onClick = onLast) {
                Icon(
                    painter = painterResource(id = R.mipmap.skip_backward),
                    contentDescription = "Backward",
                    modifier = Modifier.size(48.dp)
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

            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(id = R.mipmap.skip_forward),
                    contentDescription = "nextEpisode",
                    modifier = Modifier.size(48.dp)
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
    }
}