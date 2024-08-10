package com.silverest.remotty.server

import com.github.benmanes.caffeine.cache.Caffeine
import java.net.URL
import java.util.concurrent.TimeUnit

class ImageService {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .maximumSize(1000)
        .build<String, ByteArray>()

    fun fetchImage(url: String): ByteArray {
        return cache.get(url) {
            URL(url).openStream().use { it.readBytes() }
        }
    }
}