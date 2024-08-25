package com.silverest.remotty.common

import java.io.Serializable

data class EpisodeDetails(
    val subs: List<SubtitleTrack>,
    val chapters: List<Chapter>,
): Serializable {
    companion object {
        fun empty(): EpisodeDetails = EpisodeDetails(listOf(), listOf())
    }
}