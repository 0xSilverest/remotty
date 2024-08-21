package com.silverest.remotty.server.catalog

import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.ScrollDirection
import com.silverest.remotty.server.utils.CommandExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque
import kotlin.math.ceil
import kotlin.math.round
import kotlin.random.Random

class EpisodesService {
    private val logger = KotlinLogging.logger {}
    private val episodeCache = ConcurrentHashMap<String, EpisodeDescriptor>()

    private fun getVideoFiles(folder: File): List<String> {
        val files = mutableListOf<String>()
        val stack = ArrayDeque<File>()
        stack.add(folder)

        while (stack.isNotEmpty()) {
            val current = stack.removeFirst()
            current.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> stack.addLast(file)
                    file.extension in listOf("mkv", "mp4") -> files.add(file.absolutePath)
                }
            }
        }
        return files
    }

    fun getEpisodes(folder: File, startEpisode: Int, count: Int, scrollDirection: ScrollDirection): Triple<List<EpisodeDescriptor>, Int, Int> {
        val startLoadingTime = System.currentTimeMillis()
        val files = getVideoFiles(folder)
        val sortedFiles = files.filter { isEpisode(it) }.sorted()
        val totalEpisodes = sortedFiles.size

        val startIndex = (startEpisode - 1).coerceAtLeast(0).coerceAtMost(totalEpisodes)

        val endIndex = when {
            scrollDirection == ScrollDirection.NONE -> startIndex
            scrollDirection == ScrollDirection.UP ||
                 totalEpisodes == startIndex-> maxOf(startIndex - count, 0)
            scrollDirection == ScrollDirection.DOWN ||
                 startIndex == 1 -> minOf(startIndex + count, totalEpisodes)
            else -> startIndex
        }

        val paginatedFiles = when {
            startIndex <= endIndex -> sortedFiles.subList(startIndex, endIndex)
            else -> sortedFiles.subList(endIndex, startIndex)
        }

        val episodes = paginatedFiles.mapNotNull { filePath ->
            val relativePath = filePath.replace("${folder.absolutePath}/", "")
            val episodeNumber = extractEpisodeNumber(relativePath)?.toInt() ?: return@mapNotNull null

            episodeCache.getOrPut(filePath) {
                EpisodeDescriptor(
                    episode = episodeNumber,
                    relativePath = relativePath,
                    episodeLength = formatDuration(getVideoDurationInSeconds(filePath)),
                    thumbnail = getThumbnail(filePath)
                )
            }
        }.sortedBy { it.episode }

        val endLoadingTime = System.currentTimeMillis()
        logger.info { "Loading episodes took ${endLoadingTime - startLoadingTime} milliseconds." }

        return Triple(episodes, totalEpisodes, startIndex)
    }

    private fun getVideoDurationInSeconds(filePath: String): Int {
        val command = arrayOf(
            "ffprobe",
            "-v",
            "error",
            "-select_streams",
            "v:0",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            filePath
        )
        val process = ProcessBuilder(*command).start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readText().trim()
        process.waitFor()

        return output.toDoubleOrNull()?.toInt() ?: 0
    }

    private fun isEpisode(fileName: String): Boolean {
        val regex = Regex("""(?:[^\dS]|^)[Ee]?(\d{2,3})\D""")
        return regex.containsMatchIn(fileName)
    }

    private fun extractEpisodeNumber(fileName: String): String? {
        val regex = Regex("""(?:[^\dS]|^)[Ee]?(\d{2,3})\D""")
        val matchResult = regex.find(fileName)
        return matchResult?.groups?.get(1)?.value
    }

    private fun getThumbnail(videoFilePath: String): ByteArray? {
        val thumbnailFilePath = "/tmp/" + sanitizeFilename(videoFilePath) + ".webp"

        val thumbnailFile = File(thumbnailFilePath)
        if (thumbnailFile.exists()) {
            return thumbnailFile.readBytes()
        } else {
            val randomTime = getRandomTimestamp(getVideoDurationInSeconds(videoFilePath))
            val processBuilder = ProcessBuilder(
                "ffmpeg",
                "-ss",
                randomTime,
                "-i",
                videoFilePath,
                "-frames:v",
                "1",
                "-vf",
                "scale=192:108",
                "-c:v", "libwebp", "-lossless", "0",
                "-q:v", "80", "-loop", "0", "-an",
                thumbnailFilePath
            )
            processBuilder.redirectErrorStream(true)
            try {
                val process = processBuilder.start()
                val reader = process.inputStream.bufferedReader()
                val output = reader.readText()
                val exitCode = process.waitFor()

                if (exitCode == 0) {
                    return File(thumbnailFilePath).readBytes()
                } else {
                    println("Error generating thumbnail. FFmpeg output: $output")
                    return null
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private fun getRandomTimestamp(durationInSeconds: Int): String {
        val randomSecond = Random.nextDouble(0.0, durationInSeconds.toDouble())
        val hours = (randomSecond / 3600).toInt()
        val minutes = ((randomSecond % 3600) / 60).toInt()
        val seconds = (randomSecond % 60).toInt()
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun sanitizeFilename(filePath: String): String {
        val fileName = filePath.substringAfterLast('/')
        val baseName = fileName.substringBeforeLast('.')

        val sanitizedBaseName = baseName
            .replace(Regex("[/:*?\"<>|]"), "_")
            .take(255)

        return sanitizedBaseName
    }
}