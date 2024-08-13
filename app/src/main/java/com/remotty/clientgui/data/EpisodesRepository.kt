package com.remotty.clientgui.data

import com.remotty.clientgui.network.ClientManager
import com.silverest.remotty.common.EpisodeDescriptor
import com.silverest.remotty.common.ScrollDirection
import kotlinx.coroutines.flow.Flow

interface EpisodesRepository {
    fun getEpisodesFlow(): Flow<Triple<List<EpisodeDescriptor>, Int, Int>>
    suspend fun fetchEpisodes(title: String, page: Int, pageSize: Int, scrollDirection: ScrollDirection)
}

class EpisodesRepositoryImpl(private val clientManager: ClientManager) : EpisodesRepository {
    override fun getEpisodesFlow(): Flow<Triple<List<EpisodeDescriptor>, Int, Int>> = clientManager.episodesFlow

    override suspend fun fetchEpisodes(title: String, page: Int, pageSize: Int, scrollDirection: ScrollDirection) {
        clientManager.requestEpisodes(title, page, pageSize, scrollDirection)
    }
}