package com.silverest.remotty.common

import kotlinx.serialization.Serializable

@Serializable
data class AniObject(
    val id: Int?,
    val titleRomanji: String?,
    val titleEnglish: String?,
    val coverImageUrl: String?
)