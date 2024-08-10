package com.silverest.remotty.server

import com.silverest.remotty.common.EpisodeDescriptor
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

fun getVideoDurationInSeconds(filePath: String): Int {
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

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

fun extractEpisodeNumber(fileName: String): String? {
    val regex = Regex("""(?:[^\dS]|^)[Ee]?(\d{2,3})\D""")
    val matchResult = regex.find(fileName)
    return matchResult?.groups?.get(1)?.value
}

fun generateThumbnailWithJavaCV(videoFilePath: String, time: String = "00:00:01"): ByteArray? {
    val thumbnailFilePath = videoFilePath.replaceAfterLast("/", UUID.randomUUID().toString() + ".jpeg")
    val processBuilder = ProcessBuilder(
        "ffmpeg", "-i", videoFilePath, "-ss", time, "-vframes", "1", "-vf", "scale=240:135", thumbnailFilePath
    )
    processBuilder.redirectErrorStream(true)
    try {
        val process = processBuilder.start()
        val reader = process.inputStream.bufferedReader()
        val output = reader.readText()
        val exitCode = process.waitFor()

        if (exitCode == 0) {
            val thumbnailFile = File(thumbnailFilePath)
            try {
                val byteArray = thumbnailFile.readBytes()
                return byteArray
            } finally {
                thumbnailFile.delete()
            }
        } else {
            println("Error generating thumbnail. FFmpeg output: $output")
            return null
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}