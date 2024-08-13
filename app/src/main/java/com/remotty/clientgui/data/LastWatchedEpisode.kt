package com.remotty.clientgui.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "last_watched_episodes")
data class LastWatchedEpisode(
    @PrimaryKey val showName: String,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    @ColumnInfo(name = "last_watched_time") val lastWatchedTime: Long = System.currentTimeMillis()
)

