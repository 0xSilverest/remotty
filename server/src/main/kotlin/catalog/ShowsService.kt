package com.silverest.remotty.server.catalog

import com.github.benmanes.caffeine.cache.Caffeine
import com.silverest.remotty.common.ShowDescriptor
import com.silverest.remotty.server.network.ServerService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

data class ShowsService (
    val folder: File,
    val aniListService: AniListService,
    val serverService: ServerService
) : CoroutineScope {
    private val logger = KotlinLogging.logger {}

    val shows: MutableSet<ShowDescriptor> = mutableSetOf()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private fun loadShows(): List<ShowDescriptor>? {
        val loadingStart = System.currentTimeMillis()
        val shows = folder.listFiles()?.onEach { logger.debug { it } }?.map {
            ShowDescriptor(
                coverArt = aniListService.get(it.name.uppercase(Locale.ROOT))
                    ?.let { url -> fetchCoverArt(url, it.name) },
                name = it.name,
                rootPath = it.absolutePath
            )
        }
        val loadingEnd = System.currentTimeMillis()
        logger.info { "Shows loading took ${loadingEnd - loadingStart} ms." }
        return shows
    }

    private fun updateShows() {
        //shows.clear()
        //shows.addAll(loadShows() ?: emptyList())
        val newShows = loadShows() ?: emptyList()

        val currentShowsNames = shows.associateBy { it.name }
        val newShowsNames = newShows.associateBy { it.name }

        val showsToRemove = currentShowsNames.keys - newShowsNames.keys
        val showsToAdd = newShowsNames.keys - currentShowsNames.keys

        if (showsToRemove.isNotEmpty() || showsToAdd.isNotEmpty()) {
            shows.forEach { logger.info { it } }
            serverService.propagateModifications(
                newShows.filter { it.name in showsToAdd }.toSet(),
                shows.filter { it.name in showsToRemove }.toSet()
            )
        }

        shows.removeAll { it.name in showsToRemove }

        newShows.filter { it.name in showsToAdd }.let { shows.addAll(it) }
    }

    fun startScheduleUpdates(intervalMinutes: Long) {
        launch {
            while (isActive) {
                updateShows()
                delay(TimeUnit.MINUTES.toMillis(intervalMinutes))
            }
        }
    }

    fun stopScheduleUpdates() {
        job.cancel()
    }

    fun genFile(content: String): File =
        File("${folder.absolutePath}/${content}/")

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumSize(1000)
        .build<String, ByteArray>()

    private fun fetchCoverArt(url: String, name: String): ByteArray {
        return cache.get(url) {
            val tmpWebpPath = Path.of("/tmp/$name.webp")
            if (tmpWebpPath.exists()) {
                tmpWebpPath.toFile().readBytes()
            } else {
                val tmpPngPath = Path.of("/tmp/$name.png")

                try {
                    val prefix = "CoverArt"
                    logger.info { "Downloading $prefix for $name..." }
                    URL(url).openStream().use { inputStream: InputStream ->
                        Files.copy(inputStream, tmpPngPath)
                    }

                    val process =
                        ProcessBuilder("cwebp", tmpPngPath.absolutePathString(), "-o", tmpWebpPath.absolutePathString())
                            .start()

                    val exitCode = process.waitFor()

                    return@get if (exitCode == 0) {
                        val webpBytes = Files.readAllBytes(tmpWebpPath)

                        webpBytes
                    } else {
                        tmpPngPath.toFile().readBytes()
                    }
                } finally {
                    tmpPngPath.toFile().delete()
                }
            }
        }
    }
}