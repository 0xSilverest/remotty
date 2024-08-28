package com.silverest.remotty.server.utils

import com.silverest.remotty.common.Chapter
import com.silverest.remotty.common.SubtitleTrack
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.concurrent.thread

object CommandExecutor {
    private val logger = KotlinLogging.logger {}
    private var currentProcess: Process? = null

    private fun executeCommand(processBuilder: ProcessBuilder) {
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        //val reader = process.inputStream.bufferedReader()
        //var line: String? = reader.readLine()
        // while (line != null) {
        //     logger.info { line }
        //     line = reader.readLine()
        // }

        val exitCode = process.waitFor()
        logger.info { "Process exited with code: $exitCode" }
    }

    private fun varVolume(amount: String) = ProcessBuilder("amixer", "-q", "set", "Master", amount)

    fun increaseVolume(n: Int) {
        val command = varVolume("$n%+")
        executeCommand(command)
    }

    fun decreaseVolume(n: Int) {
        val command = varVolume("$n%-")
        executeCommand(command)
    }

    fun play(videoPath: String) {
        thread {
            cleanup()
            executeCommand(
                ProcessBuilder(
                    "mpv",
                    "--fs",
                    "--fs-screen=1",
                    "--input-ipc-server=/tmp/mpvsocket",
                    videoPath
                )
            )
        }
    }

    private fun cleanup() {
        currentProcess?.destroy()
        executeCommand(ProcessBuilder("pkill", "-6", "mpv"))
    }

    fun mute() {
        val command = varVolume("toggle")
        executeCommand(command)
    }

    fun playOrPause() {
        val command = """{ "command": ["cycle", "pause"] }"""
        mpvCommand(command)
    }

    fun seek(seconds: Int) {
        val command = """{"command": ["seek", $seconds, "relative"]}"""
        mpvCommand(command)
    }

    fun setSubtitle(id: Int?) {
        mpvCommand("""{ "command": ["set_property", "sid", ${id ?: "null"}] }""")
    }

    fun setChapter(index: Int) {
        mpvCommand("""{ "command": ["set_property", "chapter", $index] }""")
    }

    fun getSubtitleTracks(): List<SubtitleTrack> {
        val command = """{ "command": ["get_property", "track-list"] }"""
        val output = mpvCommand(command)

        val json = Json.parseToJsonElement(output)
        val trackList = json.jsonObject["data"]?.jsonArray ?: return emptyList()

        return trackList
            .filter { it.jsonObject["type"]?.jsonPrimitive?.content == "sub" }
            .mapNotNull { track ->
                val id = track.jsonObject["id"]?.jsonPrimitive?.int
                val title = track.jsonObject["title"]?.jsonPrimitive?.content
                val lang = track.jsonObject["lang"]?.jsonPrimitive?.content

                if (id != null && title != null && lang != null) {
                    SubtitleTrack(id, title, lang)
                } else {
                    null
                }
            }
    }

    fun getVideoChapters(): List<Chapter> {
        val command = """{ "command": ["get_property", "chapter-list"] }"""
        val output = mpvCommand(command)

        val json = Json.parseToJsonElement(output)
        val chapterList = json.jsonObject["data"]?.jsonArray ?: return emptyList()

        return chapterList.mapIndexed { index, chapterJson ->
            val chapterObj = chapterJson.jsonObject
            val title = chapterObj["title"]?.jsonPrimitive?.contentOrNull ?: "Chapter ${index + 1}"
            val time = formatTime(chapterObj["time"]?.jsonPrimitive?.double ?: 0.0)
            Chapter(title, time, index)
        }
    }

    private fun formatTime(seconds: Double): String {
        val hours = seconds.toInt() / 3600
        val minutes = (seconds.toInt() % 3600) / 60
        val secs = seconds.toInt() % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }


    private fun mpvCommand(command: String): String {
        val echoProcessBuilder = ProcessBuilder("echo", command)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)

        val socatProcessBuilder = ProcessBuilder("socat", "-", "/tmp/mpvsocket")
            .redirectErrorStream(true)

        val echoProcess = echoProcessBuilder.start()
        val socatProcess = socatProcessBuilder.start()

        val echoOutput = echoProcess.inputStream
        val socatInput = socatProcess.outputStream

        echoOutput.copyTo(socatInput)

        echoOutput.close()
        socatInput.close()

        echoProcess.waitFor()
        socatProcess.waitFor()

        val reader = BufferedReader(InputStreamReader(socatProcess.inputStream))
        val results = reader.readText()

        if (results.isNotEmpty()) {
            logger.info { "Output from socat process: $results" }
        }

        val errors = BufferedReader(InputStreamReader(socatProcess.errorStream)).readText()
        if (errors.isNotEmpty()) {
            logger.info { "Error from socat process: $errors" }
        }


        return results
    }

    fun close() {
        executeCommand(ProcessBuilder("pkill", "-6", "mpv"))
    }

    fun playMovie(path: String) {
        val movieDirectory = File(path)
        val videoFile = movieDirectory.listFiles()?.firstOrNull { it.extension in listOf("mkv", "mp4") }
        videoFile?.let { play(it.absolutePath) }
    }
}