package com.silverest.remotty.common

import java.io.Serializable
import kotlin.time.Duration

data class EpisodeMessage(
    override val signal: Signal,
    val episodes: List<EpisodeDescriptor>,
    val totalEpisodes: Int,
    val startEpisode: Int,
) : Serializable, IMessage(signal)
