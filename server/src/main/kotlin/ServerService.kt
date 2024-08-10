package com.silverest.remotty.server

import com.silverest.remotty.common.IMessage
import com.silverest.remotty.common.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.ServerSocket
import java.net.Socket

data class ServerService (
    val server: ServerSocket,
) {
    val clients = mutableListOf<ClientConnection>()

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

    fun acceptClient(showsService: ShowsService): ClientConnection {
        val clientConnection : ClientConnection = ClientConnection(server.accept(), showsService)
        clients.add(clientConnection)
        return clientConnection
    }

    fun propagateMessage(msg: IMessage) {

    }
}