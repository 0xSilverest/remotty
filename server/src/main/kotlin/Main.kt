package com.silverest.remotty.server

import com.silverest.remotty.server.catalog.AniListService
import com.silverest.remotty.server.catalog.EpisodesService
import com.silverest.remotty.server.catalog.ShowMetadataService
import com.silverest.remotty.server.network.ServerService
import com.silverest.remotty.server.catalog.ShowsService
import com.silverest.remotty.server.utils.ServerConfig
import com.silverest.remotty.server.network.MessagesHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.*
import java.net.SocketException

suspend fun main() {
    val logger = KotlinLogging.logger {}
    val config = ServerConfig.loadConfig()
    logger.info { "Remotty started" }

    val serverService = ServerService.createServer(6786)

    val animeFolder = File(config.animeFolder)
    val showMetadataService = ShowMetadataService(File("anime_repo.json"))
    val aniListService = AniListService(config.remoteFileUrl, config.aniListDataFile)
    val showsService = ShowsService(animeFolder, showMetadataService, aniListService, serverService)
    val episodesService = EpisodesService()

    showsService.startScheduleUpdates(1)

    val serverJob = CoroutineScope(Dispatchers.IO).launch {
        try {
            while (serverService.isRunning()) {
                val clientConnection = serverService.acceptClient(showsService)
                logger.info { "Client connected : ${clientConnection.client.inetAddress.hostAddress}" }

                val messagesHandler = MessagesHandler(showsService, episodesService, clientConnection, serverService)

                try {
                    clientConnection.getMessages { message ->
                        CoroutineScope(Dispatchers.IO).launch {
                            messagesHandler.parseMessages(animeFolder, message)
                        }
                    }

                } catch (e: EOFException) {
                    logger.info { "Client closed the connection." }
                } catch (e: SocketException) {
                    logger.info { "Client closed the connection." }
                } catch (e: IOException) {
                    logger.info { "IO Error: ${e.message}" }
                } catch (e: ClassNotFoundException) {
                    logger.info { "Class Not Found Error: ${e.message}" }
                } finally {
                    serverService.closeClient(clientConnection)
                }

                logger.info { "Client disconnected." }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Shutdown hook triggered, stopping server..." }
        serverService.close()
        logger.info { "Remotty stopped" }
    })

    serverJob.start()
    logger.info { "Server is running. Press CTRL+C to stop." }
    serverJob.join()
}