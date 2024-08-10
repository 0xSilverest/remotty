package com.silverest.remotty.server

import com.silverest.remotty.common.Signal
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

data class MessagesHandler (
    val showsService: ShowsService,
    val episodesService: EpisodesService,
    val client: ClientConnection
) {
    private val logger = KotlinLogging.logger {}

    fun parseMessages(folder: File) {
        while (true) {
            val message = client.readMessage()
            logger.info { message }

            when (message.signal) {
                Signal.SHOWS_LIST -> client.sendShows(showsService.shows)
                Signal.SEND_EPISODES ->
                    client.sendEpisodes(
                        episodesService.getEpisodes(showsService.genFile(message.content))
                    )

                Signal.INCREASE -> CommandExecutor.increaseVolume(5)
                Signal.DECREASE -> CommandExecutor.decreaseVolume(5)
                Signal.PLAY_OR_PAUSE -> CommandExecutor.playOrPause()
                Signal.SEEK_BACKWARD -> CommandExecutor.seek(-10)
                Signal.SEEK_FORWARD -> CommandExecutor.seek(10)
                Signal.SKIP_FORWARD -> CommandExecutor.seek(10)
                Signal.SKIP_BACKWARD -> CommandExecutor.seek(10)
                Signal.PLAY -> CommandExecutor.play("${folder.absolutePath}/${message.content}")
                Signal.MUTE -> CommandExecutor.mute()
                Signal.EXIT -> break
                else -> {}
            }
        }
    }
}