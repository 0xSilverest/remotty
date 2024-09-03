package com.silverest.remotty.common

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeMetadata(
    val number: String,
    val title: String?,
    val subUrl: String?,
    val rawUrl: String?
): java.io.Serializable