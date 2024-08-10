package com.silverest.remotty.server

import com.silverest.remotty.common.EpisodeDescriptor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class EpisodesService {
    private val logger = KotlinLogging.logger {}

    fun getEpisodes(folder: File): List<EpisodeDescriptor?> {
        val startLoadingTime = System.currentTimeMillis()
        val files = mutableListOf<String>()
        val stack = Stack<File>()
        stack.push(folder)

        while (stack.isNotEmpty()) {
            val current = stack.pop()

            current.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    stack.push(file)
                } else {
                    if (file.extension == "mkv" || file.extension == "mp4") {
                        files.add(file.absolutePath)
                    }
                }
            }
        }

        val episodes = runBlocking {
            files.map {
                val relativePath = it.replace("${folder.absolutePath}/", "")
                extractEpisodeNumber(relativePath)?.let { it1 ->
                    EpisodeDescriptor(
                        episode = it1.toInt(),
                        relativePath = relativePath,
                        episodeLength = formatDuration(getVideoDurationInSeconds(it)),
                        thumbnail = generateThumbnailWithJavaCV(it)
                    )
                }
            }
        }

        val endLoadingTime = System.currentTimeMillis()
        logger.info { "Loading episodes took ${endLoadingTime - startLoadingTime} milliseconds." }

        return episodes
    }
}