package com.remotty.clientgui.data

import androidx.room.*

@Dao
interface LastWatchedEpisodeDao {
    @Query("SELECT * FROM last_watched_episodes WHERE showName = :showName")
    suspend fun getLastWatchedEpisode(showName: String): LastWatchedEpisode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(lastWatchedEpisode: LastWatchedEpisode)

    @Query("UPDATE last_watched_episodes SET last_watched_time = :timestamp WHERE showName = :showName")
    suspend fun updateLastWatchedTime(showName: String, timestamp: Long = System.currentTimeMillis())
}
