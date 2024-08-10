package com.remotty.clientgui

import com.silverest.remotty.common.Message
import com.silverest.remotty.common.Signal
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

object ClientManager {
    private const val SERVER_ADDRESS = "10.0.2.2"
    private const val SERVER_PORT = 6786

    private val client: Socket = Socket(SERVER_ADDRESS, SERVER_PORT)
    val objectWriter: ObjectOutputStream = ObjectOutputStream(client.getOutputStream())
    val objectReader: ObjectInputStream = ObjectInputStream(client.getInputStream())

    fun close() {
        objectWriter.writeObject(Message(Signal.EXIT, ""))
        objectWriter.flush()
        objectReader.close()
        objectWriter.close()
        client.close()
    }

    fun sendSignal(signal: Signal, content: String = "") {
        objectWriter.writeObject(Message(signal, content))
        objectWriter.flush()
    }
}