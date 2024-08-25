package com.silverest.remotty.common

import java.io.Serializable

data class EpisodeDescriptor(
    val relativePath: String,
    val episode: Int,
    val episodeLength: String,
    val thumbnail: ByteArray?,
    var isWatched: Boolean = false
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpisodeDescriptor

        if (relativePath != other.relativePath) return false
        if (episode != other.episode) return false
        if (episodeLength != other.episodeLength) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = relativePath.hashCode()
        result = 31 * result + episode
        result = 31 * result + episodeLength.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "EpisodeDescriptor(" +
                "relativePath='$relativePath', " +
                "episode='$episode', " +
                "episodeLength='$episodeLength')"
    }
}