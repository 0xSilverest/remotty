package com.silverest.remotty.server.catalog

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.silverest.remotty.common.AniObject
import com.silverest.remotty.server.AnimeListQuery
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

class AniListService {

    companion object {
        private operator fun <K, V> HashMap<K, V>.set(title: V?, value: V?) {}
    }

    private val apolloClient = ApolloClient.Builder()
        .serverUrl("https://graphql.anilist.co")
        .build()

    private val romanjiMap: HashMap<String, String> = HashMap()
    private val englishMap: HashMap<String, String> = HashMap()

    init {
        runBlocking {
            val aniList: List<AniObject> = if (File("anilist.json").exists()) {
                loadAniListFromJson()
            } else {
                loadAniList().also {
                    saveAniListToJson(it)
                }
            }

            aniList
                .forEach {
                    it.titleRomanji?.uppercase(Locale.ROOT)?.let { it1 ->
                        it.coverImageUrl?.let { it2 ->
                            romanjiMap.put(
                                it1,
                                it2
                            )
                        }
                    }
                    it.titleEnglish?.uppercase(Locale.ROOT)?.let { it1 ->
                        it.coverImageUrl?.let { it2 ->
                            englishMap.put(
                                it1,
                                it2
                            )
                        }
                    }
                }
        }
    }

    fun get(title: String): String? {
        var url: String? = romanjiMap[title]

        if (url != null && url.isEmpty()) {
            url = englishMap[title]
        }

        return url
    }

    private suspend fun loadAniList(): List<AniObject> {
        val aniList: ArrayList<AniObject> = ArrayList()
        for (i in 1..30) {
            getAniObject(i)?.let { aniList.addAll(it) }
        }
        return aniList
    }

    private suspend fun getAniObject(page: Int, perPage: Int = 50): List<AniObject>? {
        val response = apolloClient.query(AnimeListQuery(Optional.present(page), Optional.present(perPage))).execute()
        return response.data?.Page?.media?.map {
            AniObject(
                id = it?.id,
                titleEnglish = it?.title?.english,
                titleRomanji = it?.title?.romaji,
                coverImageUrl = it?.coverImage?.extraLarge
            )
        }
    }

    private fun saveAniListToJson(aniList: List<AniObject>, fileName: String = "anilist.json") {
        val jsonString = Json.encodeToString(aniList)
        File(fileName).writeText(jsonString)
    }

    private fun loadAniListFromJson(fileName: String = "anilist.json"): List<AniObject> {
        val jsonString = File(fileName).readText()
        return Json.decodeFromString(jsonString)
    }
}