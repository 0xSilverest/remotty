package com.silverest.remotty.server.network

import com.silverest.remotty.common.ShowDescriptor
import com.silverest.remotty.common.Signal
import com.silverest.remotty.server.catalog.ShowsService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.ServerSocket

data class ServerService (
    val server: ServerSocket,
) {
    private val clients = mutableListOf<ClientConnection>()

    companion object {
        private val logger = KotlinLogging.logger {}

        fun createServer(port: Int): ServerService {
            logger.info { "Server starting..." }
            val serverService = ServerService(ServerSocket(port))
            logger.info { "Server running on port ${serverService.server.localPort}" }
            return serverService
        }
    }

    fun close() = server.close()

    fun isRunning(): Boolean = !server.isClosed

    fun closeClient(client: ClientConnection) {
        client.close()
        clients.removeIf { !client.isOnline() }
    }

    fun acceptClient(showsService: ShowsService): ClientConnection {
        val clientConnection = ClientConnection(server.accept(), showsService)
        logger.info { "Client connected: $clientConnection" }
        clients.add(clientConnection)
        return clientConnection
    }

    fun propagateModifications(newShows: Set<ShowDescriptor>, showsToRemove: Set<ShowDescriptor>) {
        clients.filter { !it.client.isClosed }.forEach {
            logger.info { "Processing $it" }
            it.sendShowsModification(
                Signal.MODIFY_SHOWS, newShows, showsToRemove
            )
        }
    }
}