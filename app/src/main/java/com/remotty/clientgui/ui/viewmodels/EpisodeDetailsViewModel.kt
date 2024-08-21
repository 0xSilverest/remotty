package com.remotty.clientgui.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotty.clientgui.data.EpisodeDetailsRepository
import com.silverest.remotty.common.EpisodeDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EpisodeDetailsViewModel (
    private val episodeDetailsRepo: EpisodeDetailsRepository,
) : ViewModel() {
    private val _details = MutableStateFlow(EpisodeDetails.empty())
    val details: StateFlow<EpisodeDetails> = _details.asStateFlow()

    init {
        viewModelScope.launch {
            episodeDetailsRepo.getDetailsFlow().collect { newDetails ->
                _details.value = newDetails
            }
        }
    }

    fun updateSubtitle(subtitleId: Int) {
        viewModelScope.launch {
            episodeDetailsRepo.updateSubs(subtitleId)
        }
    }

    fun updateChapter(chapterIndex: Int) {
        viewModelScope.launch {
            episodeDetailsRepo.updateChapter(chapterIndex)
        }
    }

    fun fetchDetails() {
        viewModelScope.launch {
            delay(1000)
            episodeDetailsRepo.fetchDetails()
        }
    }

    fun clean() {
        viewModelScope.launch {
            _details.value = EpisodeDetails.empty()
        }
    }
}