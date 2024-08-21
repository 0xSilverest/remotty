package com.remotty.clientgui.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotty.clientgui.data.ShowsRepository
import com.silverest.remotty.common.ShowDescriptor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShowsViewModel (
    private val showsRepository: ShowsRepository
) : ViewModel() {
    private val _shows = MutableStateFlow<Set<ShowDescriptor>>(emptySet())
    val shows: StateFlow<Set<ShowDescriptor>> = _shows.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredShows: StateFlow<Collection<ShowDescriptor>> = combine(shows, searchQuery) { shows, query ->
        if (query.isEmpty()) shows else {
            shows.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    init {
        viewModelScope.launch {
            showsRepository.getShowsFlow().collect { newShows ->
                Log.d("ShowsViewModel", "Received new shows: ${newShows.size}")
                _shows.value = newShows
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            showsRepository.getShowsModificationFlow().collect { (showsToAdd, showsToRemove) ->
                Log.d("ShowsViewModel", "Modifying shows - Add: ${showsToAdd.size}, Remove: ${showsToRemove.size}")
                val currentShows = _shows.value.toMutableSet()
                currentShows.addAll(showsToAdd)
                currentShows.removeAll(showsToRemove)
                _shows.value = currentShows
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun requestShowsList() {
        viewModelScope.launch {
            Log.d("ShowsViewModel", "Requesting shows list")
            _isLoading.value = true
            showsRepository.requestShowsList()
        }
    }

    fun clean() {
        Log.d("ShowsViewModel", "Cleaning shows")
        _shows.value = emptySet()
        _isLoading.value = false
    }
}