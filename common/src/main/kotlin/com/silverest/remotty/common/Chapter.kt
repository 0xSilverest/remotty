package com.silverest.remotty.common

import java.io.Serializable

data class Chapter(
    val title: String,
    val time: String,
    val index: Int
): Serializable