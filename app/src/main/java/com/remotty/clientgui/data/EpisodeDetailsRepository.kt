package com.remotty.clientgui.data

import com.remotty.clientgui.network.ClientManager
import com.silverest.remotty.common.EpisodeDetails
import kotlinx.coroutines.flow.Flow

interface EpisodeDetailsRepository {
    fun getDetailsFlow(): Flow<EpisodeDetails>
    fun fetchDetails(): Unit
    fun updateChapter(chapterIndex: Int)
    fun updateSubs(subIndex: Int)
}

class EpisodeDetailsRepositoryImpl(private val clientManager: ClientManager) : EpisodeDetailsRepository {
    override fun getDetailsFlow(): Flow<EpisodeDetails> = clientManager.detailsFlow

    override fun fetchDetails() {
        clientManager.requestEpisodeDetails()
    }

    override fun updateChapter(chapterIndex: Int) {
        clientManager.updateChapter(chapterIndex)
    }

    override fun updateSubs(subIndex: Int) {
        clientManager.updateSubs(subIndex)
    }
}