package com.silverest.remotty.server.network

import com.silverest.remotty.common.*
import com.silverest.remotty.server.catalog.ShowsService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import java.net.SocketException
import kotlin.coroutines.CoroutineContext

data class ClientConnection (
    val client: Socket,
    val showsService: ShowsService,
) : CoroutineScope {
    private val oos = ObjectOutputStream(client.getOutputStream())
    private val ois = ObjectInputStream(client.getInputStream())

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val logger = KotlinLogging.logger {}

    fun getMessages(messagesHandler: (Message) -> Unit) {
        while (isActive) {
            val message =
                ois.readObject()
            if (message is Message) {
                logger.info { message }
                messagesHandler(message)
            } else {
                throw IllegalArgumentException("Received object is not a Message")
            }
        }
    }

    fun sendEpisodes(episodes: List<EpisodeDescriptor>, totalEpisodes: Int, startEpisode: Int) {
        launch {
            oos.writeObject(EpisodeMessage(Signal.SEND_EPISODES, episodes, totalEpisodes, startEpisode))
            oos.flush()
            oos.reset()
        }
    }

    fun sendShows(signal: Signal, shows: Set<ShowDescriptor>) {
        try {
            oos.writeObject(
                ShowsMessage(
                    signal,
                    shows
                )
            )
            oos.flush()
            oos.reset()
        } catch (e: SocketException) {
            logger.error(e) { "Error while sending episodes" }
        }
    }

    fun sendDetails(subs: List<SubtitleTrack>, chapters: List<Chapter>) {
        try {
            oos.writeObject(
                DetailsMessage(
                    Signal.SEND_DETAILS,
                    EpisodeDetails(subs, chapters)
                )
            )
            oos.flush()
            oos.reset()
        } catch (e: SocketException) {
            logger.error(e) { "Error while sending episodes" }
        }
    }

    fun sendShowsModification(modifyShows: Signal, newShows: Set<ShowDescriptor>, showsToRemove: Set<ShowDescriptor>) {
        try {
            oos.writeObject(
                ShowsModificationMessage(
                    modifyShows,
                    newShows,
                    showsToRemove
                )
            )
            oos.flush()
            oos.reset()
        } catch (e: SocketException) {
            logger.error(e) { "Error while sending episodes" }
        }
    }

    fun close() {
        oos.close()
        ois.close()
        client.close()
    }

    fun isOnline() = client.isConnected
}