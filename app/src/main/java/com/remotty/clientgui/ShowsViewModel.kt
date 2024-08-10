package com.remotty.clientgui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silverest.remotty.common.ShowDescriptor
import com.silverest.remotty.common.ShowsMessage
import com.silverest.remotty.common.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import kotlin.properties.Delegates

class ShowsViewModel : ViewModel() {
    private val _shows = MutableLiveData<List<ShowDescriptor>>()
    val shows: LiveData<List<ShowDescriptor>> get() = _shows
    var lastModificationDate by Delegates.notNull<Long>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ClientManager.sendSignal(Signal.SHOWS_LIST)
            getShows(ClientManager.objectReader)
        }.start()
    }

    private fun getShows(reader: ObjectInputStream) {
        val shows = ArrayList<ShowDescriptor>()
        try {
            val showsMessage: Any? = reader.readObject()
            if (showsMessage is ShowsMessage) {
                shows.addAll(showsMessage.shows)
                _shows.postValue(shows)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}