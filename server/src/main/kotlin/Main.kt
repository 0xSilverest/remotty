package com.silverest.remotty.server

import com.silverest.remotty.common.Message
import com.silverest.remotty.common.Signal
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.*
import java.net.SocketException

fun main() {
    val logger = KotlinLogging.logger {}
    logger.info { "Remotty started" }

    val serverService = ServerService.createServer(6786)

    val folder = File("/home/silverest/Downloads/Anime")
    val aniListService = AniListService()
    val imageService = ImageService()
    val showsService = ShowsService(folder, aniListService, imageService, serverService)
    val episodesService = EpisodesService()

    showsService.startScheduleUpdates(1)

    var running = serverService.isRunning()

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Shutdown hook triggered, stopping server..." }
        running = false
        serverService.close()
        logger.info { "Remotty stopped" }
    })

    val serverThread = Thread {
        try {
            while (running) {
                val clientConnection = serverService.acceptClient(showsService)
                logger.info { "Client connected : ${clientConnection.client.inetAddress.hostAddress}" }

                val messageHandler = MessagesHandler(showsService, episodesService, clientConnection)
                try {
                    messageHandler.parseMessages(folder)
                } catch (e: EOFException) {
                    logger.info { "Client closed the connection." }
                } catch (e: SocketException) {
                    logger.info { "Client closed the connection." }
                } catch (e: IOException) {
                    logger.info { "IO Error: ${e.message}" }
                } catch (e: ClassNotFoundException) {
                    logger.info { "Class Not Found Error: ${e.message}" }
                } finally {
                    clientConnection.close()
                }

                logger.info { "Client disconnected." }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    serverThread.start()
    logger.info { "Server is running. Press CTRL+C to stop." }
    serverThread.join()
}