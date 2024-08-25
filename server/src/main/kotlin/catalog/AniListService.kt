package com.silverest.remotty.server.catalog

import com.silverest.remotty.common.AniObject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.util.*

class AniListService {

    val logger = KotlinLogging.logger {}

    companion object {
        private const val REMOTE_JSON_URL = "https://raw.githubusercontent.com/0xSilverest/anilist-fetcher/main/anilist_data.json"
        private const val LOCAL_JSON_FILE = "/home/silverest/Coding/remotty/anilist_data.json"
    }

    private val romanjiMap: Map<String, String>
    private val englishMap: Map<String, String>

    init {
        val aniList = loadAniList()
        romanjiMap = aniList.associate { it.titleRomanji?.uppercase(Locale.ROOT) to it.coverImageUrl }
            .filterKeys { it != null } as Map<String, String>
        englishMap = aniList.associate { it.titleEnglish?.uppercase(Locale.ROOT) to it.coverImageUrl }
            .filterKeys { it != null } as Map<String, String>
    }

    private fun loadAniList(): List<AniObject> {
        return when {
            File(LOCAL_JSON_FILE).exists() -> loadAniListFromJson(LOCAL_JSON_FILE)
            else -> fetchRemoteAniList()
        }.onEach { i -> logger.debug { i } }
    }

    private fun fetchRemoteAniList(): List<AniObject> {
        return try {
            val jsonString = URL(REMOTE_JSON_URL).readText()
            val aniList: List<AniObject> = Json.decodeFromString(jsonString)
            saveAniListToJson(aniList, LOCAL_JSON_FILE)
            aniList
        } catch (e: Exception) {
            println("Failed to fetch remote AniList. Using empty list.")
            emptyList()
        }
    }

    fun get(title: String): String? {
        val uppercaseTitle = title.uppercase(Locale.ROOT)
        return romanjiMap[uppercaseTitle] ?: englishMap[uppercaseTitle]
    }

    private fun saveAniListToJson(aniList: List<AniObject>, fileName: String = LOCAL_JSON_FILE) {
        val jsonString = Json.encodeToString(aniList)
        File(fileName).writeText(jsonString)
    }

    private fun loadAniListFromJson(fileName: String = LOCAL_JSON_FILE): List<AniObject> {
        val jsonString = File(fileName).readText()
        return Json.decodeFromString(jsonString)
    }
}