package com.nerf.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadedTrackEntity::class, LibraryTrackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun libraryTrackDao(): LibraryTrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nerf_music_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `library_tracks` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL,
                        `album` TEXT, `thumbnailUrl` TEXT, `durationText` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL, `sourceIdentifier` TEXT NOT NULL,
                        `playlistId` TEXT, `browseId` TEXT, `artistBrowseId` TEXT,
                        `verifiedReleaseDate` TEXT, `releaseGenre` TEXT,
                        `statsYear` INTEGER NOT NULL,
                        `playCount` INTEGER NOT NULL, `totalListenMs` INTEGER NOT NULL,
                        `lastPlayedAtEpochMs` INTEGER, `libraryAddedAtEpochMs` INTEGER,
                        `likedAtEpochMs` INTEGER, PRIMARY KEY(`id`))"""
                )
            }
        }
    }
}
