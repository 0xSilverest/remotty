package com.silverest.remotty.server.catalog

import com.silverest.remotty.common.AniObject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.util.*

class AniListService(
    private val remoteJsonUrl: String,
    private val localJsonFile: String,
) {

    private val logger = KotlinLogging.logger {}

    private val romanjiMap: Map<String, AniObject>
    private val englishMap: Map<String, AniObject>

    init {
        val aniList = loadAniList()
        romanjiMap = aniList.associateBy { it.titleRomanji?.uppercase(Locale.ROOT) }
            .filterKeys { it != null } as Map<String, AniObject>
        englishMap = aniList.associateBy { it.titleEnglish?.uppercase(Locale.ROOT) }
            .filterKeys { it != null } as Map<String, AniObject>
    }

    private fun loadAniList(): List<AniObject> {
        return when {
            File(localJsonFile).exists() -> loadAniListFromJson(localJsonFile)
            else -> fetchRemoteAniList()
        }.onEach { i -> logger.debug { i } }
    }

    private fun fetchRemoteAniList(): List<AniObject> {
        return try {
            val jsonString = URL(remoteJsonUrl).readText()
            val aniList: List<AniObject> = Json.decodeFromString(jsonString)
            saveAniListToJson(aniList, localJsonFile)
            aniList
        } catch (e: Exception) {
            println("Failed to fetch remote AniList. Using empty list.")
            emptyList()
        }
    }

    fun get(title: String): AniObject? {
        val uppercaseTitle = title.uppercase(Locale.ROOT)
        return romanjiMap[uppercaseTitle] ?: englishMap[uppercaseTitle]

    }

    private fun saveAniListToJson(aniList: List<AniObject>, fileName: String = localJsonFile) {
        val jsonString = Json.encodeToString(aniList)
        File(fileName).writeText(jsonString)
    }

    private fun loadAniListFromJson(fileName: String = localJsonFile): List<AniObject> {
        val jsonString = File(fileName).readText()
        return Json.decodeFromString(jsonString)
    }
}