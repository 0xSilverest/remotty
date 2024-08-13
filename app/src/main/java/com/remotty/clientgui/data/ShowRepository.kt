package com.remotty.clientgui.data

import com.remotty.clientgui.network.ClientManager
import com.silverest.remotty.common.ShowDescriptor
import kotlinx.coroutines.flow.Flow

interface ShowsRepository {
    fun getShowsFlow(): Flow<Set<ShowDescriptor>>
    fun getShowsModificationFlow(): Flow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>>
    suspend fun requestShowsList()}

class ShowsRepositoryImpl(private val clientManager: ClientManager) : ShowsRepository {
    override fun getShowsFlow(): Flow<Set<ShowDescriptor>> = clientManager.showsFlow

    override fun getShowsModificationFlow(): Flow<Pair<Set<ShowDescriptor>, Set<ShowDescriptor>>> =
        clientManager.showsModificationFlow

    override suspend fun requestShowsList() {
        clientManager.requestShowsList()
    }
}
