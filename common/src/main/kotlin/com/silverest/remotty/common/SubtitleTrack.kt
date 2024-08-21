package com.silverest.remotty.common

import java.io.Serializable

data class SubtitleTrack (
    val id: Int,
    val title: String,
    val lang: String
): Serializable