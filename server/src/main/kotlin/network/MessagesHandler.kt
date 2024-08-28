package com.silverest.remotty.server.network

import com.silverest.remotty.common.Message
import com.silverest.remotty.common.ScrollDirection
import com.silverest.remotty.common.Signal
import com.silverest.remotty.server.catalog.EpisodesService
import com.silverest.remotty.server.catalog.ShowsService
import com.silverest.remotty.server.utils.CommandExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MessagesHandler (
    val showsService: ShowsService,
    val episodesService: EpisodesService,
    val client: ClientConnection,
    val serverService: ServerService,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun parseMessages(folder: File, message: Message) {
        withContext(Dispatchers.IO) {
            when (message.signal) {
                Signal.SHOWS_LIST -> client.sendShows(Signal.SHOWS_LIST, showsService.shows)
                Signal.SEND_EPISODES -> {
                    val (showName, startIndex, pageSize, direction) = message.content.split(";")
                    val (episodes, totalEps, remainingPages) = episodesService.getEpisodes(
                        showsService.genFile(showName),
                        startIndex.toInt(),
                        pageSize.toInt(),
                        ScrollDirection.valueOf(direction)
                    )

                    client.sendEpisodes(showName, episodes, totalEps, remainingPages)
                }
         Signal.SEND_DETAILS -> {
                    val chapters = CommandExecutor.getVideoChapters()
                    val subs = CommandExecutor.getSubtitleTracks()

                    client.sendDetails(subs, chapters)
                }

                Signal.SKIP_CHAPTER -> CommandExecutor.setChapter(message.content.toInt())
                Signal.PUT_SUBS -> CommandExecutor.setSubtitle(message.content.toInt())
                Signal.INCREASE -> CommandExecutor.increaseVolume(5)
                Signal.DECREASE -> CommandExecutor.decreaseVolume(5)
                Signal.PLAY_OR_PAUSE -> CommandExecutor.playOrPause()
                Signal.SEEK_BACKWARD -> CommandExecutor.seek(-10)
                Signal.SEEK_FORWARD -> CommandExecutor.seek(10)
                Signal.PLAY -> CommandExecutor.play("${folder.absolutePath}/${message.content}")
                Signal.PLAY_MOVIE -> CommandExecutor.playMovie("${folder.absolutePath}/${message.content}")
                Signal.MUTE -> CommandExecutor.mute()
                Signal.CLOSE -> CommandExecutor.close()
                Signal.EXIT -> serverService.closeClient(client)
                else -> {}
            }
        }
    }
}