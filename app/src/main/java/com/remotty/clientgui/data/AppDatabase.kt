package com.remotty.clientgui.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LastWatchedEpisode::class, WatchedEpisode::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lastWatchedEpisodeDao(): LastWatchedEpisodeDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watched_episodes` (
                        `id` TEXT NOT NULL, 
                        `show_name` TEXT NOT NULL, 
                        `episode_number` INTEGER NOT NULL, 
                        `watched_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """
                )
            }
        }
    }
}