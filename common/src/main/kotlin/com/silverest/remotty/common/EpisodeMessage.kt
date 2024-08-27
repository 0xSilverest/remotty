package com.silverest.remotty.common

import java.io.Serializable

data class EpisodeMessage(
    override val signal: Signal,
    val showName: String,
    val episodes: List<EpisodeDescriptor>,
    val totalEpisodes: Int,
    val startEpisode: Int,
) : Serializable, IMessage(signal)
