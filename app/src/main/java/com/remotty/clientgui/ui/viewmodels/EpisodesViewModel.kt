package com.remotty.clientgui.ui.viewmodels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotty.clientgui.data.EpisodesRepository
import com.remotty.clientgui.data.LastWatchedEpisode
import com.remotty.clientgui.data.LastWatchedEpisodeDao
import com.silverest.remotty.common.ScrollDirection
import com.silverest.remotty.common.EpisodeDescriptor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EpisodesViewModel(
    private val episodeRepo: EpisodesRepository,
    private val lastWatchedEpisodeDao: LastWatchedEpisodeDao
) : ViewModel() {
    private val _episodes = MutableStateFlow(linkedSetOf<EpisodeDescriptor>())
    val episodes: StateFlow<List<EpisodeDescriptor>> = _episodes
        .map { it.toList().sortedBy { episode -> episode.episode } }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

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

    private fun updateEpisodes(newEpisodes: List<EpisodeDescriptor>) {
        val currentSet = _episodes.value.toMutableSet()
        currentSet.addAll(newEpisodes)
        _episodes.value = LinkedHashSet(currentSet.sortedBy { it.episode })

        if (newEpisodes.isNotEmpty()) {
            lowestLoadedEpisode = minOf(lowestLoadedEpisode, newEpisodes.minOf { it.episode })
            highestLoadedEpisode = maxOf(highestLoadedEpisode, newEpisodes.maxOf { it.episode })
        }

        updateScrollState()
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

    fun clearEpisodes() {
        _episodes.value = linkedSetOf()
        lowestLoadedEpisode = Int.MAX_VALUE
        highestLoadedEpisode = Int.MIN_VALUE
        _isLoading.value = false
        _canScrollUp.value = true
        _canScrollDown.value = true
        totalEpisodes = 0
    }

    fun startFetchingEpisodes(showName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            episodeRepo.fetchEpisodes(showName, getLastWatchedEpisode(showName), PAGE_SIZE, ScrollDirection.DOWN)
            _isLoading.value = false
        }
    }

    companion object {
        const val PAGE_SIZE = 6
    }
}