package com.remotty.clientgui.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_episodes")
data class WatchedEpisode(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "show_name") val showName: String,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int,
    @ColumnInfo(name = "watched_at") val watchedAt: Long = System.currentTimeMillis()
)