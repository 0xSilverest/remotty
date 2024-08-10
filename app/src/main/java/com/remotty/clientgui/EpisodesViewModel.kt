package com.remotty.clientgui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.Message
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.properties.Delegates

class EpisodesViewModel : ViewModel() {
    private val _episodes = MutableLiveData<List<EpisodeDescriptor>>()
    val episodes: LiveData<List<EpisodeDescriptor>> get() = _episodes
    var lastModificationDate by Delegates.notNull<Long>()

    fun fetchEpisodes(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ClientManager.objectWriter.writeObject(Message(Signal.SEND_EPISODES, title))
                ClientManager.objectWriter.flush()
                getEpisodes(ClientManager.objectReader)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getEpisodes(reader: ObjectInputStream) {
        val episodes = ArrayList<EpisodeDescriptor>()
        try {
            val episodeDescriptor: Any? = reader.readObject()
            println("Received object: $episodeDescriptor") // Debugging line
            Log.d("MainActivity", "getEpisodes: $episodeDescriptor")
            if (episodeDescriptor is List<*>) {
                episodes.addAll(episodeDescriptor.filterIsInstance<EpisodeDescriptor>())
                _episodes.postValue(episodes.sortedBy { it.episode })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearEpisodes() {
        _episodes.value = emptyList()
    }
}