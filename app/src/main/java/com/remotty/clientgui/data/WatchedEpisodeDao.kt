package com.remotty.clientgui.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {
    @Query("SELECT * FROM watched_episodes WHERE show_name = :showName")
    fun getWatchedEpisodesFlow(showName: String): Flow<List<WatchedEpisode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedEpisode(watchedEpisode: WatchedEpisode)

    @Query("DELETE FROM watched_episodes WHERE show_name = :showName AND episode_number = :episodeNumber")
    suspend fun deleteWatchedEpisode(showName: String, episodeNumber: Int)

    @Query("SELECT COUNT(*) FROM watched_episodes WHERE show_name = :showName AND episode_number = :episodeNumber")
    suspend fun isEpisodeWatched(showName: String, episodeNumber: Int): Int
}