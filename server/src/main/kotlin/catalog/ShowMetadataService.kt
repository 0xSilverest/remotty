package com.silverest.remotty.server.catalog

import com.silverest.remotty.common.ShowMetadata
import kotlinx.serialization.json.Json
import java.io.File

class ShowMetadataService (
    private val showsMetadataJson: File
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseShowsMetadata(): List<ShowMetadata> {
        val jsonFile = showsMetadataJson
        if (!jsonFile.exists()) return emptyList()

        val jsonString = jsonFile.readText()
        val showMetadata = json.decodeFromString<List<ShowMetadata>>(jsonString)
        return showMetadata
    }
}