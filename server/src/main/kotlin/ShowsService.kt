package com.silverest.remotty.server

import com.silverest.remotty.common.ShowDescriptor
import com.silverest.remotty.common.ShowsMessage
import com.silverest.remotty.common.Signal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

data class ShowsService (
    val folder: File,
    val aniListService: AniListService,
    val imageService: ImageService,
    val serverService: ServerService
) : CoroutineScope {
    private val logger = KotlinLogging.logger {}

    val shows: MutableList<ShowDescriptor> = mutableListOf()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private fun loadShows(): List<ShowDescriptor>? {
        val loadingStart = System.currentTimeMillis()
        val shows = folder.listFiles()?.onEach { logger.debug { it } }?.map {
            ShowDescriptor(
                coverArt = aniListService.get(it.name.uppercase(Locale.ROOT))
                    ?.let { url -> imageService.fetchImage(url) },
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

        val wasUpdated = showsToRemove.isNotEmpty() || showsToAdd.isNotEmpty()

        shows.removeAll { it.name in showsToRemove }

        newShows.filter { it.name in showsToAdd }.let { shows.addAll(it) }

        if (wasUpdated) {
            serverService.propagateMessage(ShowsMessage(
                Signal.SHOWS_LIST,
                shows
            ))
        }
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
}