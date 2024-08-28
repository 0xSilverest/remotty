package com.silverest.remotty.server.utils

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import java.io.File

@Serializable
data class ServerConfig (
    val port: Int = 6786,
    val animeFolder: String = System.getProperty("user.home") + "/Anime",
    val remoteFileUrl: String = "https://raw.githubusercontent.com/0xSilverest/anilist-fetcher/main/anilist_data.json",
    val aniListDataFile: String = System.getProperty("user.home") + "/.config/remotty/anilist_data.json",
) {
    companion object {
        private val CONFIG_DIR = File(System.getProperty("user.home"), ".config/remotty")
        private val CONFIG_FILE = File(CONFIG_DIR, "remotty.yaml")

        fun loadConfig(): ServerConfig {
            val yaml = Yaml.default
            return if (CONFIG_FILE.exists()) {
                val yamlString = CONFIG_FILE.readText()
                yaml.decodeFromString(serializer(), yamlString)
            } else {
                val defaultConfig = ServerConfig()
                CONFIG_DIR.mkdirs()
                CONFIG_FILE.writeText(yaml.encodeToString(serializer(), defaultConfig))
                defaultConfig
            }
        }
    }
}