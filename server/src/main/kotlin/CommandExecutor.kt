package com.silverest.remotty.server

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.thread

object CommandExecutor {
    private val logger = KotlinLogging.logger {}
    private var currentProcess: Process? = null

    private fun executeCommand(processBuilder: ProcessBuilder) {
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val reader = process.inputStream.bufferedReader()
        var line: String? = reader.readLine()
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
            executeCommand(ProcessBuilder("mpv", "--fullscreen", "--input-ipc-server=/tmp/mpvsocket", videoPath))
        }
    }

    fun playOrPause() {
        executeCommand(ProcessBuilder("xdotool", "search", "--name", "mpv", "key", "p"))
    }

    private fun cleanup() {
        currentProcess?.destroy()
        executeCommand(ProcessBuilder("pkill", "-9", "mpv"))
    }

    fun mute() {
        val command = varVolume("toggle")
        executeCommand(command)
    }

    fun seek(seconds: Int) {
        val command = """{"command": ["seek", $seconds, "relative"]}"""

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
    }
}