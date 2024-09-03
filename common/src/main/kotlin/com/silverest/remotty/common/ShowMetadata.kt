package com.silverest.remotty.common

import kotlinx.serialization.Serializable

@Serializable
data class ShowMetadata(
    val title: String,
    val episodes: List<EpisodeMetadata>
): java.io.Serializable