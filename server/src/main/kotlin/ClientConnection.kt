package com.silverest.remotty.server

import com.silverest.remotty.common.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

data class ClientConnection (
    val client: Socket,
    val showsService: ShowsService,
) {
    private val oos = ObjectOutputStream(client.getOutputStream())
    private val ois = ObjectInputStream(client.getInputStream())

    // Place definition above class declaration to make field static
    private val logger = KotlinLogging.logger {}

    fun sendMessage(message: Message) = oos.writeObject(message)

    fun readMessage(): Message {
        val receivedObject = ois.readObject()
        if (receivedObject is Message) {
            return receivedObject
        } else {
            throw IllegalArgumentException("Received object is not a Message")
        }
    }

    fun sendEpisodes(episodes: List<EpisodeDescriptor?>) {
        oos.writeObject(episodes)
        oos.flush()
    }

    fun sendShows(shows: List<ShowDescriptor>) {
        oos.writeObject(
            ShowsMessage(
                Signal.SHOWS_LIST,
                shows
            )
        )
        oos.flush()
    }

    fun close() {
        oos.close()
        ois.close()
        client.close()
    }
}