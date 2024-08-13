package com.remotty.clientgui.network

import android.content.ContentValues.TAG
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.silverest.remotty.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.EOFException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.*

class ClientManager(context: Context) {
    private val serverPort = 6786

    private lateinit var clientSocket: Socket
    private lateinit var objectWriter: ObjectOutputStream
    private lateinit var objectReader: ObjectInputStream

    private var initialized = false
    private var context: Context = context.applicationContext

    private val _episodesFlow = MutableSharedFlow<Triple<List<EpisodeDescriptor>, Int, Int>>()
    val episodesFlow: SharedFlow<Triple<List<EpisodeDescriptor>, Int, Int>> = _episodesFlow.asSharedFlow()

    private val _showsFlow = MutableSharedFlow<Set<ShowDescriptor>>()
    val showsFlow: SharedFlow<Set<ShowDescriptor>> = _showsFlow.asSharedFlow()

    private val _showsModificationFlow = MutableSharedFlow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>>()
    val showsModificationFlow: SharedFlow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>> =
        _showsModificationFlow.asSharedFlow()

    private var receiveSignalsJob: Job? = null

    fun connect(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clientSocket = Socket(ipAddress, serverPort)
                objectWriter = ObjectOutputStream(clientSocket.getOutputStream())
                objectReader = ObjectInputStream(clientSocket.getInputStream())
                initialized = true
                startReceiveSignals()
            } catch (e: IOException) {
                Log.e("ClientManager", "Connection failed: ${e.message}")
            } catch (e: ConnectException) {
                Log.e("ClientManager", "Connection failed: ${e.message}")
            }
        }
    }

    fun close() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "close: disconnecting from server...")
            stopReceivingSignals()
            objectWriter.writeObject(Message(Signal.EXIT, ""))
            objectWriter.flush()
            objectReader.close()
            objectWriter.close()
            clientSocket.close()
        }
    }

    fun sendSignal(signal: Signal, content: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            checkInitialization()
            objectWriter.writeObject(Message(signal, content))
            objectWriter.flush()
        }
    }

    fun requestEpisodes(showName: String, page: Int, pageSize: Int, scrollDirection: ScrollDirection) {
        CoroutineScope(Dispatchers.IO).launch {
            sendSignal(Signal.SEND_EPISODES, "$showName;$page;$pageSize;$scrollDirection")
        }
    }

    private fun startReceiveSignals() {
        if (receiveSignalsJob?.isActive == true) {
            return
        }

        receiveSignalsJob = CoroutineScope(Dispatchers.IO).launch {
            checkInitialization()
            while (isActive) {
                try {
                    val message: Any = objectReader.readObject()
                    if (message is IMessage) {
                        when (message) {
                            is ShowsMessage -> {
                                _showsFlow.emit(message.shows)
                            }

                            is ShowsModificationMessage -> {
                                _showsModificationFlow.emit(Pair(message.showsToAdd, message.showsToRemove))
                            }

                            is EpisodeMessage -> {
                                _episodesFlow.emit(
                                    Triple(
                                        message.episodes,
                                        message.totalEpisodes,
                                        message.startEpisode
                                    )
                                )
                            }
                        }
                    }
                } catch (e: EOFException) {
                    Log.d(TAG, "startReceiveSignals: Socket closed ${e.message}")
                    break
                } catch (e: SocketException) {
                    Log.d(TAG, "startReceiveSignals: Socket exception ${e.message}")
                    break
                }
            }
        }
    }

    private fun stopReceivingSignals() {
        receiveSignalsJob?.cancel()
        receiveSignalsJob = null
    }

    fun scanForServers(
        onServerFound: (String?) -> Unit?,
        onScanComplete: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val networkAddress = context.getNetworkAddress()
            val lastOctet = networkAddress.split('.').last().toIntOrNull() ?: 0

            val networkPrefix = networkAddress.substringBeforeLast(".")

            val rangesToScan = listOf(
                when (lastOctet) {
                    in 100..130 -> 100..130
                    in 1..30 -> 1..30
                    else -> 1..254
                }, 1..254
            )

            for (range in rangesToScan) {
                val serverIp = scanRange(networkPrefix, range)
                if (serverIp != null) {
                    withContext(Dispatchers.Main) {
                        onServerFound(serverIp)
                    }
                    break
                }
            }

            withContext(Dispatchers.Main) {
                onScanComplete()
            }
        }
    }

    private suspend fun scanRange(networkPrefix: String, range: IntRange): String? {
        return withContext(Dispatchers.IO) {
            for (i in range) {
                val ip = "$networkPrefix.$i"
                try {
                    val socket = Socket(ip, serverPort)
                    objectWriter = ObjectOutputStream(socket.getOutputStream())
                    objectWriter.writeObject(Message(Signal.EXIT, ""))
                    objectWriter.flush()
                    objectWriter.close()
                    socket.close()
                    Log.d(TAG, "scanForServers: found server IP $ip")
                    return@withContext ip
                } catch (e: IOException) {
                    Log.d("ClientManager", "scanForServers: server not found at IP $ip")
                } catch (e: ConnectException) {
                    Log.d("ClientManager", "scanForServers: server not found at IP $ip")
                }
            }
            return@withContext null
        }
    }

    private suspend fun checkInitialization() {
        withContext(Dispatchers.IO) {
            while (!initialized) {
                delay(100)
            }
        }
    }

    fun isClosed(): Boolean = clientSocket.isClosed

    fun requestShowsList() {
        CoroutineScope(Dispatchers.IO).launch {
            sendSignal(Signal.SHOWS_LIST)
        }
    }

    suspend fun isConnected(): Boolean {
        checkInitialization()
        return clientSocket.isConnected
    }

    companion object {
        fun Context.getNetworkAddress(): String {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
            if (connectivityManager is ConnectivityManager) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)


                Log.d(
                    TAG,
                    "getNetworkPrefix: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}"
                )
                return when {
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                        getWifiAddress()
                    }

                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                        getCellularAddress()
                    }

                    else -> {
                        getFallbackAddress()
                    }
                }
            }
            return getFallbackAddress()
        }

        private fun getWifiAddress(): String =
            try {
                NetworkInterface.getNetworkInterfaces()
                    .asSequence()
                    .flatMap { it.inetAddresses.asSequence() }
                    .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
                    ?: getFallbackAddress()
            } catch (ex: Exception) {
                getFallbackAddress()
            }

        private fun getCellularAddress(): String {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    for (address in addresses) {
                        if (address is Inet4Address) {
                            val ip = address.hostAddress
                            if (ip != null) {
                                return ip.substring(0, ip.lastIndexOf('.'))
                            }
                        }
                    }
                }
            }
            return getFallbackAddress()
        }

        private fun getFallbackAddress(): String = "192.168.11.100"
    }
}