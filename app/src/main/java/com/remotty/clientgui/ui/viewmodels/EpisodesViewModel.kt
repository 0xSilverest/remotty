package com.remotty.clientgui.ui.viewmodels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remotty.clientgui.data.*
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.ScrollDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EpisodesViewModel(
    private val episodeRepo: EpisodesRepository,
    private val lastWatchedEpisodeDao: LastWatchedEpisodeDao,
    private val watchedEpisodeDao: WatchedEpisodeDao
) : ViewModel() {
    private var _watchedEpisodes = MutableStateFlow<Set<Int>>(emptySet())
    val watchedEpisodes: StateFlow<Set<Int>> = _watchedEpisodes.asStateFlow()

    private val _episodes = MutableStateFlow<List<EpisodeDescriptor>>(emptyList())
    val episodes: StateFlow<List<EpisodeDescriptor>> = combine(_episodes, _watchedEpisodes) { episodes, watchedSet ->
        episodes.map { episode ->
            episode.copy(isWatched = episode.episode in watchedSet)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _nextEpisode = MutableStateFlow<EpisodeDescriptor?>(null)
    val nextEpisode: StateFlow<EpisodeDescriptor?> = _nextEpisode.asStateFlow()

    private val _previousEpisode = MutableStateFlow<EpisodeDescriptor?>(null)
    val previousEpisode: StateFlow<EpisodeDescriptor?> = _previousEpisode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canScrollUp = MutableStateFlow(false)
    val canScrollUp: StateFlow<Boolean> = _canScrollUp.asStateFlow()

    private val _canScrollDown = MutableStateFlow(true)
    val canScrollDown: StateFlow<Boolean> = _canScrollDown.asStateFlow()

    private var lowestLoadedEpisode = Int.MAX_VALUE
    private var highestLoadedEpisode = Int.MIN_VALUE
    private var totalEpisodes = 0
    private var startEpisode = 0

    private var watchedEpisodesJob: Job? = null

    init {
        viewModelScope.launch {
            episodeRepo.getEpisodesFlow().collect { (newEpisodes, total, start) ->
                totalEpisodes = total
                startEpisode = start
                updateEpisodes(newEpisodes)
                _isLoading.value = false
                updateScrollState()
            }
        }
    }

    fun fetchWatchedEpisodes(currentShowName: String) {
        watchedEpisodesJob?.cancel()
        watchedEpisodesJob = viewModelScope.launch {
            watchedEpisodeDao.getWatchedEpisodesFlow(currentShowName).collect { watchedList ->
                Log.d(TAG, "fetchWatchedEpisodes: Received ${watchedList.size} watched episodes")
                _watchedEpisodes.value = watchedList.map { it.episodeNumber }.toSet()
            }
        }
    }

    private fun updateEpisodesWatchedStatus(watchedList: List<WatchedEpisode>) {
        val currentWatchedEpisodes = _watchedEpisodes.value.toMutableSet()

        currentWatchedEpisodes.addAll(watchedList.map { it.episodeNumber }.toSet())

        _watchedEpisodes.value = currentWatchedEpisodes
    }

    private fun updateEpisodes(newEpisodes: List<EpisodeDescriptor>) {
        val currentList = _episodes.value.toMutableList()
        newEpisodes.forEach { newEpisode ->
            val index = currentList.indexOfFirst { it.episode == newEpisode.episode }
            if (index != -1) {
                currentList[index] = newEpisode
            } else {
                currentList.add(newEpisode)
            }
        }
        _episodes.value = currentList.sortedBy { it.episode }

        if (newEpisodes.isNotEmpty()) {
            lowestLoadedEpisode = minOf(lowestLoadedEpisode, newEpisodes.minOf { it.episode })
            highestLoadedEpisode = maxOf(highestLoadedEpisode, newEpisodes.maxOf { it.episode })
        }
    }

    fun updateNextAndLast(showName: String, episode: EpisodeDescriptor) {
        viewModelScope.launch {
            val nextEpisodeFlow = episodes.value.find { it.episode == episode.episode + 1 }
            _nextEpisode.value = nextEpisodeFlow

            if (_nextEpisode.value != null
                && _nextEpisode.value?.episode!! < totalEpisodes
                && _nextEpisode.value?.episode == highestLoadedEpisode
            ) {
                fetchEpisodes(showName, ScrollDirection.DOWN)
            }

            val previousEpisodeFlow = episodes.value.find { it.episode == episode.episode - 1 }
            _previousEpisode.value = previousEpisodeFlow

            if (_previousEpisode.value != null
                && _previousEpisode.value?.episode!! > 1
                && _previousEpisode.value?.episode == lowestLoadedEpisode
            ) {
                fetchEpisodes(showName, ScrollDirection.DOWN)
            }

            Log.d(TAG, "updateNext: ${_nextEpisode.value}")
            Log.d(TAG, "updateLast: ${_previousEpisode.value}")
        }
    }

    private fun updateScrollState() {
        _canScrollUp.value = lowestLoadedEpisode > 1
        _canScrollDown.value = highestLoadedEpisode < totalEpisodes
    }

    private suspend fun getLastWatchedEpisode(showName: String): Int {
        return lastWatchedEpisodeDao.getLastWatchedEpisode(showName)?.episodeNumber ?: 1
    }

    fun fetchEpisodes(showName: String, direction: ScrollDirection) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            val episodeToFetch = when (direction) {
                ScrollDirection.DOWN -> highestLoadedEpisode
                ScrollDirection.UP -> maxOf(1, lowestLoadedEpisode)
                ScrollDirection.NONE -> lowestLoadedEpisode
            }

            episodeRepo.fetchEpisodes(showName, episodeToFetch, PAGE_SIZE, direction)
            _isLoading.value = false
        }
    }

    fun updateLastWatchedEpisode(showName: String, episodeNumber: Int) {
        viewModelScope.launch {
            lastWatchedEpisodeDao.insertOrUpdate(LastWatchedEpisode(showName, episodeNumber))
        }
    }

    fun clean() {
        _episodes.value = emptyList()
        _watchedEpisodes.value = emptySet()
        lowestLoadedEpisode = Int.MAX_VALUE
        highestLoadedEpisode = Int.MIN_VALUE
        _isLoading.value = false
        _canScrollUp.value = true
        _canScrollDown.value = true
        totalEpisodes = 0
        watchedEpisodesJob?.cancel()
        watchedEpisodesJob = null
    }

    fun startFetchingEpisodes(showName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            episodeRepo.fetchEpisodes(showName, getLastWatchedEpisode(showName), PAGE_SIZE, ScrollDirection.DOWN)
            _isLoading.value = false
        }
    }

    fun updateWatchedStatus(showName: String, episode: Int, isWatched: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "updateWatchedStatus: $episode $isWatched")
            if (isWatched) {
                watchedEpisodeDao.insertWatchedEpisode(
                    WatchedEpisode(
                        "$showName:$episode",
                        showName,
                        episode
                    )
                )
                _watchedEpisodes.update { it + episode }
            } else {
                watchedEpisodeDao.deleteWatchedEpisode(showName, episode)
                _watchedEpisodes.update { it - episode }
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 6
    }
}