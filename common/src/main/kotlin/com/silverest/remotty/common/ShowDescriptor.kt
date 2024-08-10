package com.silverest.remotty.common

import java.io.Serializable

data class ShowDescriptor(
    val coverArt: ByteArray?,
    val name: String,
    val rootPath: String,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShowDescriptor

        if (coverArt != null) {
            if (other.coverArt == null) return false
            if (!coverArt.contentEquals(other.coverArt)) return false
        } else if (other.coverArt != null) return false
        if (name != other.name) return false
        if (rootPath != other.rootPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coverArt?.contentHashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + rootPath.hashCode()
        return result
    }

    override fun toString(): String {
        return "ShowDescriptor(name='$name', rootPath='$rootPath')"
    }
}