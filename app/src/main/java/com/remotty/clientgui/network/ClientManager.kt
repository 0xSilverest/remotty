package com.remotty.clientgui.network

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.silverest.remotty.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private val _episodeDetails = MutableSharedFlow<EpisodeDetails>()
    val detailsFlow: SharedFlow<EpisodeDetails> = _episodeDetails.asSharedFlow()

    private val _showsModificationFlow = MutableSharedFlow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>>()
    val showsModificationFlow: SharedFlow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>> =
        _showsModificationFlow.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    private val _latency = MutableStateFlow<Long>(0)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    private var scanJob: Job? = null
    private var receiveSignalsJob: Job? = null

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("ClientPreferences", Context.MODE_PRIVATE)

    private var keepAliveJob: Job? = null
    private val keepAliveInterval = 30000L
    private val pongTimeOut = 5000L

    fun connect(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                clientSocket = Socket(ipAddress, serverPort)
                objectWriter = ObjectOutputStream(clientSocket.getOutputStream())
                objectReader = ObjectInputStream(clientSocket.getInputStream())
                initialized = true
                startReceiveSignals()
                startKeepAlive()
                _connectionStatus.value = true
                saveLastIpAddress(ipAddress)
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
            stopKeepAlive()
            try {
                objectWriter.writeObject(Message(Signal.EXIT, ""))
                objectWriter.flush()
                objectReader.close()
                objectWriter.close()
                clientSocket.close()
            } catch (e: IOException) {
                Log.d(TAG, "close: socket already off")
            } catch (e: UninitializedPropertyAccessException) {
                Log.d(TAG, "close: socket not initialized")
            }
            _connectionStatus.value = false
        }
    }

    fun sendSignal(signal: Signal, content: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkInitialization()
                objectWriter.writeObject(Message(signal, content))
                objectWriter.flush()
            } catch (e: IOException) {
                Log.d(TAG, "sendSignal: ${e.message}")
                _connectionStatus.value = false
            }
        }
    }

    fun requestEpisodes(showName: String, page: Int, pageSize: Int, scrollDirection: ScrollDirection) {
        CoroutineScope(Dispatchers.IO).launch {
            sendSignal(Signal.SEND_EPISODES, "$showName;$page;$pageSize;$scrollDirection")
        }
    }

    fun requestEpisodeDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            sendSignal(Signal.SEND_DETAILS)
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
                        Log.d(TAG, "Received message: $message")
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

                            is DetailsMessage -> {
                                _episodeDetails.emit(
                                    message.details
                                )
                            }

                            is KeepAliveMessage -> {
                                    val pongTime = System.currentTimeMillis()
                                    val rtt = pongTime - message.timestamp
                                    _latency.value = rtt
                                    Log.d(TAG, "Round-trip time: $rtt ms")
                            }
                        }
                    }
                } catch (e: EOFException) {
                    Log.d(TAG, "startReceiveSignals: Socket closed ${e.message}")
                    _connectionStatus.value = false
                    break
                } catch (e: SocketException) {
                    Log.d(TAG, "startReceiveSignals: Socket exception ${e.message}")
                    _connectionStatus.value = false
                    break
                }
            }
        }
    }

    private fun stopReceivingSignals() {
        receiveSignalsJob?.cancel()
        receiveSignalsJob = null
    }

    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }


    fun scanForServers(
        onServerFound: (String?) -> Unit?,
        onScanComplete: () -> Unit
    ) {
        scanJob = CoroutineScope(Dispatchers.IO).launch {
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

    fun updateSubs(subIndex: Int) {
        sendSignal(Signal.PUT_SUBS, "$subIndex")
    }

    fun updateChapter(chapter: Int) {
        sendSignal(Signal.SKIP_CHAPTER, "$chapter")
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun saveLastIpAddress(ipAddress: String) {
        sharedPreferences.edit().putString("last_ip_address", ipAddress).apply()
    }

    fun getLastIpAddress(): String {
        return sharedPreferences.getString("last_ip_address", "") ?: ""
    }

    private fun startKeepAlive() {
        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(keepAliveInterval)
                try {
                    sendPing()
                } catch (e: IOException) {
                    Log.e(TAG, "Keep-alive failed: ${e.message}")
                    _connectionStatus.value = false
                    reconnect()
                    break
                }
            }
        }
    }

    private suspend fun sendPing() {
        withContext(Dispatchers.IO) {
            objectWriter.writeObject(KeepAliveMessage(message = "ping"))
            objectWriter.flush()
            withTimeout(pongTimeOut) {
                while (isActive) {
                    if (_latency.value != 0L) {
                        _latency.value = 0L // Reset for next ping
                        break
                    }
                    delay(100)
                }
            }
        }
    }

    private fun reconnect() {
        close()
        val lastIpAddress = getLastIpAddress()
        if (lastIpAddress.isNotEmpty()) {
            connect(lastIpAddress)
        }
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